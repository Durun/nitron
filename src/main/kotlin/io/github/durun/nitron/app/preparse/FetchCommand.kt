package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.util.logger
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Path

class FetchCommand : CliktCommand(name = "preparse-fetch") {

    private val customConfig: Path? by option("--config")
        .path(readable = true)
    private val config = (customConfig ?: Path.of("config/nitron.json"))
        .let { NitronConfigLoader.load(it) }
    private val workingDir: File by option("--dir")
        .file(folderOkay = true, fileOkay = false)
        .defaultLazy { Path.of("tmp").toFile() }
    private val dbFile: Path by argument(name = "DATABASE", help = "Database file")
        .path(writable = true)

    private val bufferSize: Int by option("-b")
        .int()
        .default(1000)

    private val log by logger()

    override fun run() {
        val db = SQLiteDatabase.connect(dbFile)
        val dbUtil = DbUtil(db)
        val gitUtil = GitUtil(workingDir)

        val repos = dbUtil.getRepositoryInfos()
        log.info { "Fetch list:\n${repos.joinToString("\n").prependIndent("\t")}" }

        repos.forEach { repo ->
            val git = gitUtil.openRepository(repo.url)
            gitUtil.checkoutMainBranch(git)

            dbUtil.clearOldRows(repo.id)

            val extensions = repo.fileExtensions(config)
            val filter = { path: String -> extensions.any { path.endsWith(it) } }
            val commitPairs = gitUtil.zipCommitWithParent(git)

            runBlocking(Dispatchers.Default) {
                val commitInfos = commitPairs.asSequence().mapIndexed { i, (commit, parent) ->
                    async {
                        val info = gitUtil.createCommitInfo(git, commit, parent, filter)
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