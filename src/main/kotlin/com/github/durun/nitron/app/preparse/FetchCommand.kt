package com.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import com.github.durun.nitron.inout.database.SQLiteDatabase
import com.github.durun.nitron.inout.model.preparse.CommitTable
import com.github.durun.nitron.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.nio.file.Path

class FetchCommand : CliktCommand(name = "preparse-fetch") {

    private val customConfig: Path? by option("--config")
        .path(mustBeReadable = true)
    private val workingDir: File by option("--dir")
        .file(canBeFile = false, canBeDir = true)
        .defaultLazy { Path.of("tmp").toFile() }
    private val branch: String? by option("--branch")
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(mustBeWritable = true)
        .multiple()

    private val startDate: DateTime by option("--start-date", help = "date (dd:mm:yyyy)")
        .convert { it.parseToDateTime() }
        .defaultLazy { DateTime(0) }
    private val endDate: DateTime by option("--end-date", help = "date (dd:mm:yyyy)")
        .convert { it.parseToDateTime() }
        .defaultLazy { DateTime(Long.MAX_VALUE) }

    private val isVerbose: Boolean by option("--verbose").flag()
    private val isDebug: Boolean by option("--debug").flag()

    private val log by logger()

    override fun toString(): String = "<preparse-fetch>"

    override fun run() {
        LogLevel = when {
            isVerbose -> Log.Level.VERBOSE
            isDebug -> Log.Level.DEBUG
            else -> Log.Level.INFO
        }

        dbFiles.forEach { dbFile ->
            runCatching {
                log.info { "Start DB=$dbFile" }
                processOneDB(dbFile)
            }.onFailure {
                it.printStackTrace()
            }
            log.info { "Finish DB=$dbFile" }
        }
    }

    private fun processOneDB(dbFile: Path) {
        val config = (customConfig ?: Path.of("config/nitron.json"))
            .let { NitronConfigLoader.load(it) }
        val db = SQLiteDatabase.connect(dbFile)
        val dbUtil = DbUtil(db)
        val gitUtil = GitUtil(workingDir, branch)

        val repos = dbUtil.getRepositoryInfos()
        log.info { "Fetch list:\n${repos.joinToString("\n").prependIndent("\t")}" }

        repos.forEach { repo ->
            val git = gitUtil.openRepository(repo.url)
            gitUtil.checkoutMainBranch(git)

            val existCommits = transaction(db) {
                CommitTable.selectAll()
                    .asSequence()
                    .map { it[CommitTable.hash] }
                    .toSet()
            }

            val extensions = repo.fileExtensions(config)
            val filter = { path: String -> extensions.any { path.endsWith(it) } }
            val commitPairs = git.zipCommitWithParent()

            runBlocking(Dispatchers.Default) {
                val commitInfos = commitPairs.mapIndexed { i, (commit, parent) ->
                    async {
                        if (!(commit.committerIdent.`when` isExclusiveIn startDate..endDate)) return@async null
                        val commitHash = commit.id.name
                        if (existCommits.contains(commitHash)) return@async null
                        val info = git.createCommitInfo(commit, parent, filter)
                        info.files.forEach { it.checksum } // pre-compute
                        if (i % 100 == 0) log.info { "Made commit: ${repo.url} $i / ${commitPairs.size}" }
                        info
                    }
                }.toMutableSet()

                runBlocking(Dispatchers.IO) {
                    while (commitInfos.isNotEmpty()) {
                        if (commitInfos.any { it.isActive }) delay(5000)
                        val doneList = commitInfos.removeAndGetIf { it.isCompleted }
                            .mapNotNull { it.await() }
                        if (doneList.isNotEmpty()) dbUtil.batchInsertCommitInfos(repo, doneList)
                        /*
                        doneList.forEachIndexed { i, it ->
                            dbUtil.insertCommitInfo(repo, it)
                            log.verbose { "  Writing: $i / ${doneList.size}" }
                        }
                         */
                        log.info { "Wrote commit: ${repo.url} ${doneList.size}" }
                    }
                }
            }
        }
    }
}