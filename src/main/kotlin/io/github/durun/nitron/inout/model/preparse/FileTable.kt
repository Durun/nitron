package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

object FileTable : IntIdTable("files") {
    val commit: Column<EntityID<Int>> = reference("commit", CommitTable)
    val path: Column<String> = text("path")
    val checksum: Column<String> = varchar("checksum", 40)
}