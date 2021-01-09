package io.github.durun.nitron.inout.model.table

import io.github.durun.nitron.inout.model.Revision
import io.github.durun.nitron.inout.model.ammoniaDateTimeFormatter
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime


object BugfixRevisions : ReadableTable<Revision>("bugfixrevisions") {
    val software: Column<String> = text("software").primaryKey()
    val id: Column<String> = text("id").primaryKey()
    val date: Column<String> = text("date")
    val message: Column<String> = text("message")
    val author: Column<String> = text("author")

    override fun read(row: ResultRow): Revision = Revision(
            softwareName = row[software],
            commitHash = row[id],
            date = LocalDateTime.parse(row[date], ammoniaDateTimeFormatter),
            commitMessage = row[message],
            author = row[author]
    )
}