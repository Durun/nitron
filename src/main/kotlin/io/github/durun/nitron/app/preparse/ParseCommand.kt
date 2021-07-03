package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.*
import io.github.durun.nitron.util.Log
import io.github.durun.nitron.util.LogLevel
import io.github.durun.nitron.util.logger
import io.github.durun.nitron.util.parseToDateTime
import kotlinx.coroutines.*
import org.eclipse.jgit.api.Git
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.snt.inmemantlr.exceptions.ParsingException
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class ParseCommand : CliktCommand(name = "preparse") {

    private val customConfig: Path? by option("--config")
        .path(mustBeReadable = true)
    private val config = (customConfig ?: Path.of("config/nitron.json"))
        .let { NitronConfigLoader.load(it) }
    private val workingDir: File by option("--dir")
        .file(canBeFile = false, canBeDir = true)
        .defaultLazy { Path.of("tmp").toFile() }
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(mustBeWritable = true)
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

    private val isVerbose: Boolean by option("--verbose").flag()
    private val isDebug: Boolean by option("--debug").flag()

    private val log by logger()

    override fun toString(): String = "<preparse>"

    @kotlin.io.path.ExperimentalPathApi
    override fun run() {
        LogLevel = when {
            isVerbose -> Log.Level.VERBOSE
            isDebug -> Log.Level.DEBUG
            else -> Log.Level.INFO
        }

        log.info { "Available languages: ${config.langConfig.keys}" }
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

    @kotlin.io.path.ExperimentalPathApi
    private fun processOneDB(dbFile: Path) {
        val db = SQLiteDatabase.connect(dbFile)
        val dbUtil = DbUtil(db)

        config.langConfig.entries.forEach { (name, config) ->
            check(dbUtil.isLanguageConsistent(name, config)) { "Invalid language: $name" }
        }
        log.debug { "Language check OK" }

        val repos = transaction(db) {
            val rows =
                if (repoUrl.isNotEmpty()) RepositoryTable.select { RepositoryTable.url inList repoUrl.map { it.toString() } }
                else RepositoryTable.selectAll()
            rows.map { RepositoryInfo(it[RepositoryTable.id], URL(it[RepositoryTable.url]), it[RepositoryTable.langs]) }
        }

        // list asts table
        repos.forEach { repo ->
            dbUtil.prepareAstTable(repo, config, startDate..endDate)
        }

        // normalize
        log.debug { "Normalizing 'asts' table" }
        transaction(db) {
            AstTable.setNullOnAbsentContent()
        }

        // parse
        log.info { "Repository list: ${repos.map { it.url }}" }

        repos.forEach {
            log.info { "Start repository: ${it.url}" }
            processRepository(dbUtil, it.url)
            log.info { "Done repository: ${it.url}" }
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
        log.debug { "Counted remain jobs: $jobCount" }

        val count = AtomicInteger(0)
        val jobs = transaction(db) {
            dbUtil.queryAbsentAst(repoId, timeRange = startDate..endDate)
        }
        log.debug { "Loaded jobs: ${jobs.size}" }

        runBlocking(Dispatchers.Default) {
            val parsing: MutableList<Deferred<ParseJobResult?>> = jobs
                .mapIndexed { index, it ->
                    async {
                        val result = calcJob(git, parseUtil, it) ?: return@async null
                        log.verbose { "Parsed: $repoUrl $index / $jobCount: $it" }
                        result
                    }
                }.toMutableList()

            runBlocking(Dispatchers.IO) {
                while (parsing.isNotEmpty()) {
                    log.verbose { "Wait..." }
                    delay(5000)
                    val doneIndices = parsing.mapIndexedNotNull { i, job ->
                        i.takeIf { job.isCompleted }
                    }
                    val doneList: MutableList<ParseJobResult> = mutableListOf()
                    doneIndices.asReversed().forEach { i ->
                        parsing.removeAt(i).await()?.let { doneList += it }
                    }
                    if (doneList.isNotEmpty()) {
                        writeJobResult(db, doneList)
                    }
                    log.info { "($url) Wrote ${doneList.size} (${count.addAndGet(doneList.size)}/$jobCount)" }
                }
            }
        }
    }

    private fun writeJobResult(db: Database, jobs: Collection<ParseJobResult>) {
        transaction(db) {
            jobs.forEach { (astId, parseResult) ->
                val contentId = parseResult
                    ?.let { AstContentTable.insertIfAbsentAndGetId(it) }
                    ?: AstTable.FAILED_TO_PARSE
                AstTable.updateContent(astId, contentId)
            }
        }
    }

    private fun calcJob(git: Git, parseUtil: ParseUtil, job: ParseJobInfo): ParseJobResult? {
        val code = git.readFile(job.fileObjectId)
            ?: run {
                log.warn { "Can't read file: $job" }
                return null
            }
        val langConfig = config.langConfig[job.lang]
            ?: run {
                log.warn { "Can't get language config: $job" }
                return null
            }
        val parsed = runCatching { parseUtil.parseText(code, job.lang, langConfig) }
            .onFailure {
                System.err.println("Failed to parse: $job $it")
                if (it !is ParsingException) it.printStackTrace(System.err)
            }
            .getOrNull()
        log.verbose { "Success parsing: $job" }
        return ParseJobResult(job.astId, parsed)
    }
}