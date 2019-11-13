package io.github.durun.nitron.inout.model.cpanalyzer.table

import io.github.durun.nitron.inout.model.cpanalyzer.Revision
import io.github.durun.nitron.inout.model.cpanalyzer.ammoniaDateFormat
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow


object BugfixRevisions : ReadableTable<Revision>("bugfixrevisions") {
    val software: Column<String> = text("software").primaryKey()
    val id: Column<String> = text("id").primaryKey()
    val date: Column<String> = text("date")
    val message: Column<String> = text("message")
    val author: Column<String> = text("author")

    override fun read(row: ResultRow, alias: Alias<ReadableTable<Revision>>?): Revision {
        fun <T> Column<T>.get(): T = row[alias?.get(this) ?: this]
        return Revision(
                softwareName = software.get(),
                commitHash = id.get(),
                date = ammoniaDateFormat.parse(date.get()),
                commitMessage = message.get(),
                author = author.get()
        )
    }
}