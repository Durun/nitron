package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.CommitTable
import io.github.durun.nitron.inout.model.preparse.FileTable
import io.github.durun.nitron.inout.model.preparse.RepositoryTable
import io.github.durun.nitron.util.logger
import kotlinx.coroutines.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.Date
import kotlin.streams.asStream

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


            val commitPairs = gitUtil.zipCommitWithParent(git)

            val commits = gitUtil.createCommitInfoSequence(git) { path -> extensions.any { path.endsWith(it) } }

            val filter = { path: String -> extensions.any { path.endsWith(it) } }


            runBlocking(Dispatchers.Default) {
                val commitInfos = commitPairs.asSequence().mapIndexed { i, (commit, parent) ->
                    async {
                        val info = gitUtil.createCommitInfo(git, commit, parent, filter)
                        if (i % 100 == 0) log.info { "Made commit: $i" }
                        //dbUtil.insertCommitInfo(repo, info)
                        //if (i % 100 == 0) log.info { "Wrote commit: $i" }
                        info
                    }
                }
                runBlocking {
                    val buffer = commitInfos.toList().map { it.await() }
                    dbUtil.batchInsertCommitInfos(repo, buffer)
                }
            }
        }
    }
}