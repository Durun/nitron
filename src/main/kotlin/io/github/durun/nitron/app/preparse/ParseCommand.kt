package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.*
import io.github.durun.nitron.util.logger
import io.github.durun.nitron.util.parseToDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class ParseCommand : CliktCommand(name = "preparse") {

    private val customConfig: Path? by option("--config")
        .path(readable = true)
    private val config = (customConfig ?: Path.of("config/nitron.json"))
        .let { NitronConfigLoader.load(it) }
    private val workingDir: File by option("--dir")
        .file(folderOkay = true, fileOkay = false)
        .defaultLazy { Path.of("tmp").toFile() }
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(writable = true)
        .multiple()

    private val repoUrl: List<URL> by option("--repository", help = "Git repository name (owner/project)")
        .convert {
            val gitUrl = if (it.endsWith(".git")) it else "$it.git"
            URL(gitUrl)
        }.multiple()

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

    override fun toString(): String = "<preparse $dbFiles --repository=$repoUrl>"

    @kotlin.io.path.ExperimentalPathApi
    override fun run() {
        dbFiles.forEach { dbFile->
            runCatching {
                log.info { "Start DB=$dbFile" }
                processOneDB(dbFile)
            }.onFailure {
                it.printStackTrace()
            }
            log.info { "Finish DB=$dbFile" }
        }
    }

    @kotlin.io.path.ExperimentalPathApi
    private fun processOneDB(dbFile: Path) {
        val db = SQLiteDatabase.connect(dbFile)
        val dbUtil = DbUtil(db)

        log.info { "Available languages: ${config.langConfig.keys}" }
        config.langConfig.entries.forEach { (name, config) ->
            check(dbUtil.isLanguageConsistent(name, config)) { "Invalid language: $name" }
        }
        log.info { "Language check OK" }

        // list asts table
        dbUtil.prepareAstTable(config, startDate..endDate)

        // normalize
        log.info { "Normalizing 'asts' table" }
        transaction(db) {
            AstTable.setNullOnAbsentContent()
        }

        // parse
        val repos = repoUrl.ifEmpty {   // if empty, all repositories
            transaction(db) {
                RepositoryTable.selectAll()
                    .map { URL(it[RepositoryTable.url]) }
            }
        }

        log.info { "Repository list: $repos" }

        repos.forEach {
            log.info { "Start repository: $it" }
            processRepository(dbUtil, it)
            log.info { "Done repository: $it" }
        }
    }

    private fun processRepository(dbUtil: DbUtil, url: URL) {
        val db = dbUtil.db

        val (repoId, repoUrl) = transaction(db) {
            RepositoryTable
                .select { RepositoryTable.url eq url.toString() }
                .map { it[RepositoryTable.id] to it[RepositoryTable.url] }
                .firstOrNull() ?: throw NoSuchElementException("$url is not in DB. Try 'preparse-register' first.")
        }

        val git = GitUtil(workingDir).openRepository(URL(repoUrl))
        log.info { "Opened: $url" }

        val parseUtil = ParseUtil(config)
        val jobCount = transaction(db) { dbUtil.countAbsentAst(repoId, timeRange = startDate..endDate) }
        val count = AtomicInteger(0)
        do {
            val jobs: List<ParseJobInfo> =
                transaction(db) { dbUtil.queryAbsentAst(repoId, limit = bufferSize, timeRange = startDate..endDate) }
            log.verbose { "Got ${jobs.size} jobs" }

            runBlocking(Dispatchers.Default) {
                jobs.map {
                    async {
                        processJob(git, parseUtil, db, it)
                        log.info { "Done: $repoUrl ${count.addAndGet(1)} / $jobCount" }
                    }
                }
                    .map { it.await() }
            }
        } while (jobs.isNotEmpty())


    }

    private fun processJob(git: Git, parseUtil: ParseUtil, db: Database, job: ParseJobInfo) {
        val code = git.readFile(job.fileObjectId)
            ?: return log.warn { "Can't read file: $job" }
        val langConfig = config.langConfig[job.lang]
            ?: return log.warn { "Can't get language config: $job" }
        val parsed = runCatching { parseUtil.parseText(code, job.lang, langConfig) }
            .onFailure {
                log.warn { "Failed to parse: $job ${it.message}" }
                synchronized(db) {
                    transaction(db) {
                        AstTable.updateContent(job.astId, AstTable.FAILED_TO_PARSE)
                    }
                }
            }
            .getOrNull() ?: return
        synchronized(db) {
            transaction(db) {
                val contentId = AstContentTable.insertIfAbsentAndGetId(parsed)
                AstTable.updateContent(job.astId, contentId)
            }
        }
        log.verbose { "Done: $job" }
    }
}