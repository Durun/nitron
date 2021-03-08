package io.github.durun.nitron.inout.model.preparse

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.net.URL

object RepositoryTable : IntIdTable("repositories") {
    val name: Column<String> = text("name")
    val url: Column<String> = text("url")
        .uniqueIndex("repositories_url")
    val langs: Column<String> = text("languages")    // comma delimited language names
}

fun RepositoryTable.insertAndGetId(url: URL, langs: List<String>): EntityID<Int> {
    return this.insertAndGetId {
        it[this.name] = url.file
        it[this.url] = url.toString()
        it[this.langs] = langs.joinToString(",")
    }
}