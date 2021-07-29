package com.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insertAndGetId
import org.joda.time.DateTime

object CommitTable : IntIdTable("commits") {
    val repository: Column<EntityID<Int>> = reference("repository", RepositoryTable)
    val hash: Column<String> = varchar("hash", 40)
    val message: Column<String> = text("message")
    val date: Column<DateTime> = date("date")
    val author: Column<String> = text("author")
}

fun CommitTable.insertAndGetId(
    repositoryID: EntityID<Int>,
    hash: String,
    message: String,
    date: DateTime,
    author: String
): EntityID<Int> {
    return insertAndGetId {
        it[this.repository] = repositoryID
        it[this.hash] = hash
        it[this.message] = message
        it[this.date] = date
        it[this.author] = author
    }
}