package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
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
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URL
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


    private val log by logger()

    override fun run() {
        val db = SQLiteDatabase.connect(dbFile)
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(CommitTable, FileTable)
        }

        val repos: List<Pair<EntityID<Int>, URL>> = transaction(db) {
            RepositoryTable
                .slice(RepositoryTable.id, RepositoryTable.url)
                .selectAll()
                .map { it[RepositoryTable.id] to URL(it[RepositoryTable.url]) }
        }
        log.info { "Fetch list:\n${repos.joinToString("\n").prependIndent("\t")}" }
        repos.forEach { (repoId, repoUrl) ->
            transaction { clearOldRows(repoId) }
            val git = openRepository(repoUrl)
            checkoutMainBranch(git)
            getCommitSequence(git).forEach {
                println("Calling sequence: $it")
                transaction(db) { processCommitInfo(repoId, it) }
            }
        }
    }

    private fun openRepository(repoUrl: URL): Git {
        val repoDir = workingDir.resolve(repoUrl.file.trim('/'))
        return runCatching {
            log.info { "Opening: $repoUrl" }
            Git.open(repoDir)
        }.recover {
            log.info { "Cloning: $repoUrl" }
            Git.cloneRepository()
                .setDirectory(repoDir)
                .setURI(repoUrl.toString())
                .call()
        }.getOrThrow()
    }

    private fun checkoutMainBranch(git: Git) {
        val branches: List<Ref> = git.branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()
        val mainBranch = branches
            .find {
                val simpleName = it.name.split('/').last()
                simpleName == "main" || simpleName == "master"
            }
        log.info { "Detected main branch: $mainBranch" }
        mainBranch?.let {
            git.checkout()
                .setName(it.name)
                .call()
        }
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
            }
        }
        log.info { "Insert 'commits': $commitId" }

        commitInfo.files.forEach { fileInfo ->
            val fileId = FileTable.insertIgnore {
                it[commit] = commitId
                it[path] = fileInfo.path
            }
            log.info { "Insert 'files': $fileId" }
        }
    }
}

data class CommitInfo(
    val id: String,
    val message: String,
    val files: Collection<FileInfo>
)

data class FileInfo(
    val path: String,
    val readText: () -> String
)


private fun getCommitSequence(git: Git): Sequence<CommitInfo> {
    val commitPairs = git.log()
        .all()
        .call()
        .zipWithNext()
    require(commitPairs.isNotEmpty()) { "Commits are less than 2" }
    val initialCommit = commitPairs.last().second
    val allPairs = commitPairs + listOf(initialCommit to null)
    return allPairs.asSequence().map { (commit, parent) ->
        CommitInfo(
            id = commit.id.name,
            message = commit.fullMessage,
            files = detectCommitInfo(git.repository, commit, parent) { true }
        )
    }
}

private fun detectCommitInfo(
    repository: Repository,
    commit: RevCommit,
    parent: RevCommit?,
    filter: (fileName: String) -> Boolean
): Collection<FileInfo> {
    if (parent != null) {
        val entries = DiffFormatter(DisabledOutputStream.INSTANCE)
            .let {
                it.setRepository(repository)
                it.setDiffComparator(RawTextComparator.DEFAULT)
                it.isDetectRenames = true
                val entries = it.scan(parent, commit)
                it.close()
                entries
            }
        val reader = repository.newObjectReader()
        return entries
            .filter { filter(it.newPath) }
            .map {
                FileInfo(path = it.newPath) {
                    reader.open(it.newId.toObjectId(), Constants.OBJ_BLOB)
                        .cachedBytes
                        .decodeToString()
                }
            }
    } else {
        // if commit is the initial commit
        val treewalk = TreeWalk(repository)
            .apply {
                addTree(commit.tree)
                isRecursive = true
            }
        return mutableListOf<FileInfo>().apply {
            while (treewalk.next()) {
                val info = FileInfo(treewalk.pathString) {
                    repository.open(treewalk.getObjectId(0))
                        .cachedBytes.decodeToString()
                }
                add(info)
            }
        }
    }
}