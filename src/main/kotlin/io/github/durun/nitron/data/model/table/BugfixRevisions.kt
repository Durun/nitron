package io.github.durun.nitron.data.model.table

import io.github.durun.nitron.data.model.Revision
import io.github.durun.nitron.data.model.ammoniaDateFormat
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table


object BugfixRevisions: ReadableTable<Revision>("bugfixrevisions")
{
    val software = text("software").primaryKey()
    val id = text("id").primaryKey()
    val date = text("date")
    val message = text("message")
    val author = text("author")

    override fun read(row: ResultRow) = Revision(
            softwareName = row[software],
            commitHash = row[id],
            date = ammoniaDateFormat.parse(row[date]),
            commitMessage = row[message],
            author = row[author]
    )
}