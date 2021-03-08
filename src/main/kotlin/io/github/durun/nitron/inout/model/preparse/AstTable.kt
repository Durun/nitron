package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*

object AstTable : IntIdTable("asts") {
    val file: Column<EntityID<Int>> = reference("file", FileTable)
        .uniqueIndex()
    val language: Column<EntityID<Int>> = reference("language", LanguageTable)
    val content: Column<EntityID<Int>?> = optReference("content", AstContentTable, onDelete = ReferenceOption.SET_NULL)

    val FAILED_TO_PARSE: EntityID<Int> = EntityID(-1, AstContentTable)
}

fun AstTable.insertAndGetId(
    fileId: EntityID<Int>,
    languageId: EntityID<Int>,
    contentId: EntityID<Int>?
): EntityID<Int> {
    return insertAndGetId {
        it[this.file] = fileId
        it[this.language] = languageId
        contentId?.let { id -> it[this.content] = id }
    }
}

fun AstTable.updateContent(astId: EntityID<Int>, contentId: EntityID<Int>): Int {
    return update({ AstTable.id eq astId }) {
        it[content] = contentId
    }
}

fun AstTable.setNullOnAbsentContent(): Int {
    val deletedContents = AstTable
        .leftJoin(AstContentTable, { content }, { id })
        .select { AstContentTable.id.isNull() }
        .adjustSlice { slice(content) }
    return AstTable.update({ content.inSubQuery(deletedContents) }) {
        it[this.content] = null
    }
}