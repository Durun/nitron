package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.inout.model.preparse.CommitTable
import io.github.durun.nitron.inout.model.preparse.FileTable
import io.github.durun.nitron.inout.model.preparse.RepositoryTable
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL

internal class DbUtil(
    val db: Database
) {
    private val log by logger()

    init {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(CommitTable, FileTable)
        }
    }

    fun clearOldRows(repoTableID: EntityID<Int>) {
        transaction(db) {
            val toDeleteCommits = CommitTable.select { CommitTable.repository eq repoTableID }
                .adjustSlice { slice(CommitTable.id) }
            FileTable.deleteWhere {
                FileTable.commit.inSubQuery(toDeleteCommits)
            }
        }
        log.info { "Cleared 'files' in repository $repoTableID" }

        transaction(db) {
            CommitTable.deleteWhere {
                CommitTable.repository eq repoTableID
            }
        }
        log.info { "Cleared 'commits' in repository $repoTableID" }
    }

    fun getRepositoryInfos(): List<RepositoryInfo> = transaction(db) {
        RepositoryTable.selectAll().map {
            RepositoryInfo(
                id = it[RepositoryTable.id],
                url = URL(it[RepositoryTable.url]),
                commaDelimitedLangs = it[RepositoryTable.langs]
            )
        }
    }

    fun insertCommitInfo(repositoryInfo: RepositoryInfo, commitInfo: CommitInfo) = transaction(db) {
        val commitId =
            CommitTable.insertAndGetId {
                it[repository] = repositoryInfo.id
                it[hash] = commitInfo.id
                it[message] = commitInfo.message
                it[date] = commitInfo.date
                it[author] = commitInfo.author
            }
        log.verbose { "Insert 'commits': $commitId" }


        FileTable.batchInsert(commitInfo.files) {
            this[FileTable.commit] = commitId
            this[FileTable.path] = it.path
            this[FileTable.checksum] = MD5.digest(it.readText()).toString()
        }
        log.verbose { "Insert 'files' in $commitId" }
    }


    fun batchInsertCommitInfos(repositoryInfo: RepositoryInfo, commitInfos: List<CommitInfo>) = transaction(db) {
        val first = commitInfos.first()
        val commitId =
            CommitTable.insertAndGetId {
                it[repository] = repositoryInfo.id
                it[hash] = first.id
                it[message] = first.message
                it[date] = first.date
                it[author] = first.author
            }
        CommitTable.batchInsert(commitInfos.drop(1)) {
            this[CommitTable.repository] = repositoryInfo.id
            this[CommitTable.hash] = it.id
            this[CommitTable.message] = it.message
            this[CommitTable.date] = it.date
            this[CommitTable.author] = it.author
        }
        log.verbose { "Insert 'commits': $commitId" }

        commitInfos.forEachIndexed { i, commitInfo ->
            FileTable.batchInsert(commitInfo.files) {
                this[FileTable.commit] = EntityID(commitId.value + i, commitId.table)
                this[FileTable.path] = it.path
                this[FileTable.checksum] = MD5.digest(it.readText()).toString()
            }
        }
        log.verbose { "Insert 'files' in $commitId" }
    }
}