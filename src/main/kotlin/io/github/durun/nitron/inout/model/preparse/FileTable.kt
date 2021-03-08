package io.github.durun.nitron.inout.model.preparse

import io.github.durun.nitron.core.MD5
import org.eclipse.jgit.lib.ObjectId
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insertAndGetId

object FileTable : IntIdTable("files") {
    val commit: Column<EntityID<Int>> = reference("commit", CommitTable)
    val path: Column<String> = text("path")
    val objectId: Column<String> = varchar("objectId", 40)
    val checksum: Column<String> = varchar("checksum", 40)
}

fun FileTable.insertAndGetId(commitID: EntityID<Int>, path: String, objectId: ObjectId, checksum: MD5): EntityID<Int> {
    return insertAndGetId {
        it[this.commit] = commitID
        it[this.path] = path
        it[this.objectId] = objectId.name
        it[this.checksum] = checksum.toString()
    }
}