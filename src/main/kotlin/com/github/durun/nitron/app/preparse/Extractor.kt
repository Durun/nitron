package com.github.durun.nitron.app.preparse

import com.github.durun.nitron.core.AstSerializers
import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.config.NitronConfig
import com.github.durun.nitron.core.toByteArray
import com.github.durun.nitron.inout.database.SQLiteDatabase
import com.github.durun.nitron.inout.model.preparse.*
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.sql.Blob

class Extractor(
    val config: NitronConfig,
    val db: Database
) {
    companion object {
        fun open(config: NitronConfig, cacheDBFile: Path): Extractor = Extractor(
            config, db = SQLiteDatabase.connect(cacheDBFile)
        )
    }

    private fun getLangId(langName: String): EntityID<Int>? {
        return transaction(db) {
            LanguageTable.select { LanguageTable.name eq langName }
                .map { it[LanguageTable.id] }
                .firstOrNull()
        }
    }

    private fun getFileId(repoName: String, commitId: String, path: String): EntityID<Int>? {
        val repoId: EntityID<Int> = transaction(db) {
            RepositoryTable.select { RepositoryTable.name eq repoName }
                .map { it[RepositoryTable.id] }
                .firstOrNull()
        } ?: return null
        return transaction(db) {
            FileTable
                .innerJoin(CommitTable, { commit }, { id })
                .select { (FileTable.path eq path) and (CommitTable.hash eq commitId) and (CommitTable.repository eq repoId) }
                .map { it[FileTable.id] }
                .firstOrNull()
        }
    }

    private fun getFileId(checksum: String): EntityID<Int>? {
        return transaction(db) {
            FileTable.select { FileTable.checksum eq checksum }
                .map { it[FileTable.id] }
                .firstOrNull()
        }
    }

    private fun getBlob(langId: EntityID<Int>, fileId: EntityID<Int>): Blob? {
        val contentId: EntityID<Int> = transaction(db) {
            AstTable.select { (AstTable.file eq fileId) and (AstTable.language eq langId) }
                .map { it[AstTable.content] }
                .firstOrNull()
        } ?: return null
        if (contentId == AstTable.FAILED_TO_PARSE) throw Exception("Failed to parse. FileID: $fileId")
        return transaction(db) {
            AstContentTable.select { AstContentTable.id eq contentId }
                .map { it[AstContentTable.content] }
                .firstOrNull()
        }
    }

    fun getAst(repoName: String, commitId: String, path: String, langName: String, types: NodeTypePool): AstNode? {
        val langId: EntityID<Int> = getLangId(langName) ?: return null
        val fileId: EntityID<Int> = getFileId(repoName, commitId, path) ?: return null
        val blob: Blob = getBlob(langId, fileId) ?: return null
        val json = blob.toByteArray().ungzip()
        return AstSerializers.json(types).decodeFromString(json)
    }

    fun getAst(checksum: String, langName: String, types: NodeTypePool): AstNode? {
        val langId: EntityID<Int> = getLangId(langName) ?: run { println("DB: lang not found: $langName"); return null }
        val fileId: EntityID<Int> = getFileId(checksum) ?: run { println("DB: file not found: $checksum"); return null }
        val blob = getBlob(langId, fileId) ?: return null
        val json = blob.toByteArray().ungzip()
        return AstSerializers.json(types).decodeFromString(json)
    }
}