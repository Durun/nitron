package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.*
import io.github.durun.nitron.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URL
import java.nio.file.Path

class ParseCommand : CliktCommand(name = "preparse") {

    private val customConfig: Path? by option("--config")
        .path(readable = true)
    private val config = (customConfig ?: Path.of("config/nitron.json"))
        .let { NitronConfigLoader.load(it) }
    private val workingDir: File by option("--dir")
        .file(folderOkay = true, fileOkay = false)
        .defaultLazy { Path.of("tmp").toFile() }
    private val dbFile: Path by argument(name = "DATABASE", help = "Database file")
        .path(writable = true)
    private val repoUrl: List<URL> by option("--repository", help = "Git repository name (owner/project)")
        .convert {
            val gitUrl = if (it.endsWith(".git")) it else "$it.git"
            URL(gitUrl)
        }.multiple()
    private val allFlag: Boolean by option("--all").flag()

    private val log by logger()


    @kotlin.io.path.ExperimentalPathApi
    override fun run() {
        val db = SQLiteDatabase.connect(dbFile)
        val dbUtil = DbUtil(db)

        log.info { "Available languages: ${config.langConfig.keys}" }
        config.langConfig.entries.forEach { (name, config) ->
            check(dbUtil.isLanguageConsistent(name, config)) { "Invalid language: $name" }
            log.verbose { "Language check OK: $name" }
        }

        // list asts table
        dbUtil.prepareAstTable(config)

        // normalize
        transaction(db) {
            AstTable.setNullOnAbsentContent()
        }

        // parse
        log.info { "Repository list: $repoUrl" }
        repoUrl.forEach {
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

        val parseUtil = ParseUtil(git, config)


        do {
            val jobs: List<ParseJobInfo> = transaction(db) { dbUtil.queryAbsentAst(repoId, limit = 500) }
            log.info { "Got ${jobs.size} jobs" }

            runBlocking(Dispatchers.Default) {
                jobs.map { async { processJob(parseUtil, db, it) } }
                    .map { it.await() }
            }
        } while (jobs.isNotEmpty())


    }

    private fun processJob(parseUtil: ParseUtil, db: Database, job: ParseJobInfo) {
        val code = parseUtil.readFile(job.fileObjectId)
            ?: return log.warn { "Can't read file: $job" }
        val langConfig = config.langConfig[job.lang]
            ?: return log.warn { "Can't get language config: $job" }
        val parsed = runCatching { parseUtil.parseText(code, job.lang, langConfig) }
            .onFailure { log.warn { "Failed to parse: $job ${it.message}" } }
            .getOrNull() ?: return
        synchronized(db) {
            transaction(db) {
                val contentId = AstContentTable.insertIfAbsentAndGetId(parsed)
                AstTable.updateContent(job.astId, contentId)
            }
        }
        log.info { "Done: $job" }
    }
}