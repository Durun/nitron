package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

object CommitTable : IntIdTable("commits") {
    val repository: Column<EntityID<Int>> = reference("repository", RepositoryTable)
        .uniqueIndex("commit")
    val hash: Column<String> = varchar("hash", 40)
        .uniqueIndex("commit")
    val message: Column<String> = text("message")
}
