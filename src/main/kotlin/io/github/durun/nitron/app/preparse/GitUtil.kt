package io.github.durun.nitron.app.preparse

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
import org.joda.time.DateTime
import java.io.File
import java.net.URL

internal class GitUtil(
    val workingDir: File
) {
    private val log by logger()
    val mainBranchNames: List<String> = listOf("main", "master")

    fun openRepository(repoUrl: URL): Git {
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

    fun checkoutMainBranch(git: Git) {
        val branches: List<Ref> = git.branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()
        val mainBranch = branches
            .find {
                val simpleName = it.name.split('/').last()
                mainBranchNames.contains(simpleName)
            }
        log.info { "Detected main branch: $mainBranch" }
        mainBranch?.let {
            git.checkout()
                .setName(it.name)
                .call()
        }
    }

    /**
     * @return 組(コミット, 親コミット)のリストです。最初のコミットの親にはnullが入ります。
     */
    fun zipCommitWithParent(git: Git): List<Pair<RevCommit, RevCommit?>> {
        val commitPairs = git.log()
            .all()
            .call()
            .zipWithNext()
        require(commitPairs.isNotEmpty()) { "Commits are less than 2" }
        val initialCommit = commitPairs.last().second
        return commitPairs + listOf(initialCommit to null)
    }

    fun createCommitInfoSequence(git: Git, filter: (filePath: String) -> Boolean): Sequence<CommitInfo> {
        return zipCommitWithParent(git).asSequence()
            .map { (commit, parent) ->
                CommitInfo(
                    id = commit.id.name,
                    message = commit.fullMessage,
                    date = DateTime(commit.getDate()),
                    author = commit.authorIdent.name,
                    files = createFileInfos(git.repository, commit, parent, filter)
                )
            }
    }
    fun createCommitInfo(git: Git, commit: RevCommit, parent: RevCommit?, filter: (filePath: String) -> Boolean): CommitInfo {
        return CommitInfo(
            id = commit.id.name,
            message = commit.fullMessage,
            date = DateTime(commit.getDate()),
            author = commit.authorIdent.name,
            files = createFileInfos(git.repository, commit, parent, filter)
        )
    }

    private fun createFileInfos(
        repository: Repository,
        commit: RevCommit,
        parent: RevCommit?,
        filter: (filePath: String) -> Boolean
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
                    val objId = treewalk.getObjectId(0)
                        .takeIf { filter(treewalk.pathString) }
                        .takeUnless { it == ObjectId.zeroId() }
                    val info = objId?.let {
                        FileInfo(treewalk.pathString) {
                            repository.open(it)
                                .cachedBytes.decodeToString()
                        }
                    }
                    info?.let { add(it) }
                }
            }
        }
    }
}