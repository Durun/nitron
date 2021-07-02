package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.NitronConfig
import io.github.durun.nitron.inout.model.preparse.*
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.net.URL

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
                this[FileTable.checksum] = it.checksum.toString()
            }
        }
        log.verbose { "Insert 'files' in $commitId" }
    }

    fun insertCommitInfo(repositoryInfo: RepositoryInfo, commitInfo: CommitInfo) = transaction(db) {
        val commitId = CommitTable.insertAndGetId(
            repositoryID = repositoryInfo.id,
            hash = commitInfo.id,
            message = commitInfo.message,
            date = commitInfo.date,
            author = commitInfo.author
        )
        FileTable.batchInsert(commitInfo.files, ignore = true) {
            this[FileTable.commit] = commitId
            this[FileTable.path] = it.path
            this[FileTable.objectId] = it.objectId.name()
            this[FileTable.checksum] = it.checksum.toString()
        }
    }

    @kotlin.io.path.ExperimentalPathApi
    fun isLanguageConsistent(langName: String, langConfig: LangConfig): Boolean {
        val checksum = langConfig.parserConfig.checksum()

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

    fun prepareAstTable(config: NitronConfig, timeRange: ClosedRange<DateTime>) {

        // Language name : id
        val langs = transaction(db) {
            LanguageTable.selectAll()
                .associate { it[LanguageTable.name] to it[LanguageTable.id] }
        }

        langs.entries.forEach { (lang, langId) ->
            val extensions = config.langConfig[lang]!!.extensions
            transaction(db) {
                FileTable.innerJoin(CommitTable).select {  // files with correct extension
                    extensions.fold<String, Op<Boolean>>(Op.FALSE) { expr, ext ->
                        expr or (FileTable.path like "%$ext")
                    } and notExists(
                        AstTable.selectAll().adjustWhere { FileTable.id eq AstTable.file }
                    ) and
                            (CommitTable.date greaterEq timeRange.start) and
                            (CommitTable.date lessEq timeRange.endInclusive)

                }
                    .forEachIndexed { i, it ->
                        if (i % 10000 == 0) log.info { "Preparing 'asts' rows ($lang): $i" }
                        val fileId = it[FileTable.id]
                        AstTable.insertIgnore {
                            it[file] = fileId
                            it[language] = langId
                        }
                    }
            }
        }
        log.info { "Inserted 'asts' rows" }
    }

    fun queryAbsentAst(
        repositoryId: EntityID<Int>,
        timeRange: ClosedRange<DateTime> = DateTime(0)..DateTime(Long.MAX_VALUE)
    ): List<ParseJobInfo> {
        return AstTable
            .innerJoin(FileTable, { file }, { id })
            .innerJoin(CommitTable, { FileTable.commit }, { id })
            .innerJoin(LanguageTable, { AstTable.language }, { id })
            .select {
                whereAbsentAst(
                    repositoryId = repositoryId,
                    timeRange = timeRange
                )
            }
            //.limit(limit)
            .reversed()
            .map { ParseJobInfo(repositoryId, it[AstTable.id], it[FileTable.objectId], it[LanguageTable.name]) }
    }

    fun countAbsentAst(
        repositoryId: EntityID<Int>,
        timeRange: ClosedRange<DateTime> = DateTime(0)..DateTime(Long.MAX_VALUE)
    ): Int {
        return AstTable
            .innerJoin(FileTable, { file }, { id })
            .innerJoin(CommitTable, { FileTable.commit }, { id })
            .innerJoin(LanguageTable, { AstTable.language }, { id })
            .select {
                whereAbsentAst(
                    repositoryId = repositoryId,
                    timeRange = timeRange
                )
            }
            .count()
    }

    private fun whereAbsentAst(repositoryId: EntityID<Int>, timeRange: ClosedRange<DateTime>): Op<Boolean> {
        return CommitTable.repository eq repositoryId and
                AstTable.content.isNull() and
                (CommitTable.date greaterEq timeRange.start) and
                (CommitTable.date lessEq timeRange.endInclusive)
    }
}