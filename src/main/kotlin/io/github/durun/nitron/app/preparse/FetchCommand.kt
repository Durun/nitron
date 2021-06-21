package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.CommitTable
import io.github.durun.nitron.util.isExclusiveIn
import io.github.durun.nitron.util.logger
import io.github.durun.nitron.util.parseToDateTime
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.nio.file.Path

class FetchCommand : CliktCommand(name = "preparse-fetch") {

    private val customConfig: Path? by option("--config")
        .path(mustBeReadable = true)
    private val config = (customConfig ?: Path.of("config/nitron.json"))
        .let { NitronConfigLoader.load(it) }
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

    private val bufferSize: Int by option("-b")
        .int()
        .default(1000)

    private val log by logger()

    override fun toString(): String = "<preparse-fetch $dbFiles>"

    override fun run() {
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
                val commitInfos = commitPairs.asSequence()
                    .filter { (commit, _) -> commit.committerIdent.`when` isExclusiveIn startDate..endDate }
                    .filterNot { (commit, _) ->
                        val commitHash = commit.id.name
                        existCommits.contains(commitHash)
                    }
                    .mapIndexed { i, (commit, parent) ->
                        async {
                            val info = git.createCommitInfo(commit, parent, filter)
                            if (i % 100 == 0) log.info { "Made commit: $i" }
                            info
                        }
                    }
                runBlocking {
                    val buffers: Sequence<List<Deferred<CommitInfo>>> = commitInfos.chunked(bufferSize)
                    buffers.forEachIndexed { i, buf ->
                        dbUtil.batchInsertCommitInfos(repo, buf.awaitAll())
                        log.info { "Wrote commit: ${i * bufferSize}" }
                    }
                }
            }
        }
    }
}