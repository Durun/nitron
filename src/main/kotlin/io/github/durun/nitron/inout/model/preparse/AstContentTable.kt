package io.github.durun.nitron.inout.model.preparse

import io.github.durun.nitron.app.preparse.gzip
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.toBlob
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import java.sql.Blob

object AstContentTable : IntIdTable("ast_contents") {
    val content: Column<Blob> = blob("content")
    val checksum: Column<String> = varchar("checksum", 40)
        .uniqueIndex()
}

fun AstContentTable.insertAndGetId(content: String): EntityID<Int> {
    return insertAndGetId {
        it[this.content] = content.gzip().toBlob()
        it[this.checksum] = MD5.digest(content).toString()
    }
}

fun AstContentTable.insertIfAbsentAndGetId(content: String): EntityID<Int> {
    val checksum = MD5.digest(content).toString()
    return  this
        .select { AstContentTable.checksum eq checksum }
        .map { it[AstContentTable.id] }
        .firstOrNull()
    ?: insertAndGetId {
        it[this.content] = content.gzip().toBlob()
        it[this.checksum] = checksum
    }
}