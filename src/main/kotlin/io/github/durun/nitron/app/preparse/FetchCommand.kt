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
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.Date

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
        val gitUtil = GitUtil(workingDir)
        val db = SQLiteDatabase.connect(dbFile)
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(CommitTable, FileTable)
        }

        val repos = transaction(db) {
            RepositoryTable
                .slice(RepositoryTable.id, RepositoryTable.url, RepositoryTable.langs)
                .selectAll()
                .map { Triple(it[RepositoryTable.id], URL(it[RepositoryTable.url]), it[RepositoryTable.langs]) }
        }
        log.info { "Fetch list:\n${repos.joinToString("\n").prependIndent("\t")}" }
        repos.forEach { (repoId, repoUrl, langs) ->
            val extensions = getExtensions(langs)
            val fileFilter = { name: String -> extensions.any { name.endsWith(it) } }
            transaction { clearOldRows(repoId) }
            val git = gitUtil.openRepository(repoUrl)
            gitUtil.checkoutMainBranch(git)
            var cnt = 0
            gitUtil.createCommitInfoSequence(git, fileFilter).forEach {
                transaction(db) { processCommitInfo(repoId, it) }
                cnt++
                if (cnt % 100 == 0) log.info { "Wrote commits: $cnt" }
            }
        }
    }

    private fun getExtensions(langs: String): List<String> {
        val langList = langs.split(',')
        return langList.mapNotNull {
            config.langConfig[it]?.extensions
        }.flatten()
    }

    private fun clearOldRows(repoTableID: EntityID<Int>) {
        transaction {
            val toDeleteCommits = CommitTable.select { CommitTable.repository eq repoTableID }
                .adjustSlice { slice(CommitTable.id) }
            FileTable.deleteWhere {
                FileTable.commit.inSubQuery(toDeleteCommits)
            }
        }
        log.info { "Cleared 'files' in repository $repoTableID" }

        transaction {
            CommitTable.deleteWhere {
                CommitTable.repository eq repoTableID
            }
        }
        log.info { "Cleared 'commits' in repository $repoTableID" }
    }

    private fun processCommitInfo(repoTableID: EntityID<Int>, commitInfo: CommitInfo) {
        val commitId = transaction {
            CommitTable.insertAndGetId {
                it[repository] = repoTableID
                it[hash] = commitInfo.id
                it[message] = commitInfo.message
                it[date] = commitInfo.date
                it[author] = commitInfo.author
            }
        }
        log.verbose { "Insert 'commits': $commitId" }

        commitInfo.files.forEach { fileInfo ->
            val content = fileInfo.readText()
            val fileId = FileTable.insertIgnore {
                it[commit] = commitId
                it[path] = fileInfo.path
                it[checksum] = MD5.digest(content).toString()
            }
            log.verbose { "Insert 'files': $fileId" }
        }
    }
}