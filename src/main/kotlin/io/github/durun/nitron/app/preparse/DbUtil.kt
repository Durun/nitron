package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.NitronConfig
import io.github.durun.nitron.inout.model.preparse.*
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import kotlin.io.path.readText

internal class DbUtil(
    val db: Database
) {
    private val log by logger()

    init {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(
                RepositoryTable,
                CommitTable,
                FileTable,
                LanguageTable,
                AstTable,
                AstContentTable
            )
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

    fun batchInsertCommitInfos(repositoryInfo: RepositoryInfo, commitInfos: List<CommitInfo>) = transaction(db) {
        val first = commitInfos.first()

        val commitId = CommitTable.insertAndGetId(
            repositoryID = repositoryInfo.id,
            hash = first.id,
            message = first.message,
            date = first.date,
            author = first.author
        )
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
                this[FileTable.objectId] = it.objectId.name()
                this[FileTable.checksum] = MD5.digest(it.readText()).toString()
            }
        }
        log.verbose { "Insert 'files' in $commitId" }
    }

    @kotlin.io.path.ExperimentalPathApi
    fun isLanguageConsistent(langName: String, langConfig: LangConfig): Boolean {
        val paths = langConfig.grammar.grammarFilePaths + langConfig.grammar.utilJavaFilePaths
        val checksum = paths.map { MD5.digest(it.readText()).toString() }
            .sorted()
            .reduce { a, b -> a + b }
            .let { MD5.digest(it) }

        val correctSum = transaction(db) {
            val rows = LanguageTable.select { LanguageTable.name eq langName }
                .map { it[LanguageTable.checksum] }
            check(rows.size <= 1) { "Languages must be distinct. Check 'languages' table." }
            rows.firstOrNull()
        }

        return if (correctSum != null) checksum.toString() == correctSum
        else {
            transaction(db) {
                LanguageTable.insert {
                    it[name] = langName
                    it[this.checksum] = checksum.toString()
                }
            }
            true
        }
    }

    fun detectLangFromExtension(path: String, config: NitronConfig): String? {
        return config.langConfig.entries.find { (_, config) ->
            config.extensions.any { path.endsWith(it) }
        }?.let { (name, _) -> name }
    }

    fun prepareAstTable(config: NitronConfig) {

        // Language name : id
        val langs = transaction(db) {
            LanguageTable.selectAll()
                .associate { it[LanguageTable.name] to it[LanguageTable.id] }
        }

        val fileCount = transaction(db) {
            FileTable.selectAll().count()
        }
        var count = 0
        transaction(db) {
            FileTable.selectAll()
                .forEach {
                    val path = it[FileTable.path]
                    val langName = detectLangFromExtension(path, config)
                    val langId = langs[langName]!!
                    val fileId = it[FileTable.id]
                    AstTable.insertIgnore {
                        it[file] = fileId
                        it[language] = langId
                    }
                    count++
                    if (count % 10000 == 0) log.info { "Preparing 'asts' rows: $count / $fileCount" }
                }
        }
        log.info { "Inserted 'asts' rows" }
    }

    fun queryAbsentAst(repositoryId: EntityID<Int>, limit: Int = 100): List<ParseJobInfo> {
        return AstTable
            .innerJoin(FileTable, { file }, { id })
            .innerJoin(CommitTable, { FileTable.commit }, { id })
            .innerJoin(LanguageTable, { AstTable.language }, { id })
            .slice(CommitTable.repository, AstTable.id, FileTable.objectId, LanguageTable.name, AstTable.content)
            .select { CommitTable.repository eq repositoryId and AstTable.content.isNull() }
            .reversed()
            .take(limit)
            .map { ParseJobInfo(repositoryId, it[AstTable.id], it[FileTable.objectId], it[LanguageTable.name]) }
    }

    fun countAbsentAst(repositoryId: EntityID<Int>): Int {
        return AstTable
            .innerJoin(FileTable, { file }, { id })
            .innerJoin(CommitTable, { FileTable.commit }, { id })
            .innerJoin(LanguageTable, { AstTable.language }, { id })
            .slice(CommitTable.repository, AstTable.id, FileTable.objectId, LanguageTable.name, AstTable.content)
            .select { CommitTable.repository eq repositoryId and AstTable.content.isNull() }
            .count()
    }
}