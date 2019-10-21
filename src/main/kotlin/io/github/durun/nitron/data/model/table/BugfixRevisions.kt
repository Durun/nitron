package io.github.durun.nitron.data.model.table

import io.github.durun.nitron.data.model.Revision
import io.github.durun.nitron.data.model.ammoniaDateFormat
import org.jetbrains.exposed.sql.ResultRow


object BugfixRevisions: ReadableTable<Revision>("bugfixrevisions")
    val software: Column<String> = text("software").primaryKey()
    val id: Column<String> = text("id").primaryKey()
    val date: Column<String> = text("date")
    val message: Column<String> = text("message")
    val author: Column<String> = text("author")

    override fun read(row: ResultRow): Revision = Revision(
            softwareName = row[software],
            commitHash = row[id],
            date = ammoniaDateFormat.parse(row[date]),
            commitMessage = row[message],
            author = row[author]
    )
}