package io.github.durun.nitron.inout.model.table

import io.github.durun.nitron.core.ast.node.lineRangeOf
import io.github.durun.nitron.core.toBlob
import io.github.durun.nitron.inout.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.nio.file.Paths
import java.sql.Blob
import java.time.LocalDateTime
import javax.sql.rowset.serial.SerialBlob


object Changes : ReadWritableTable<Change>("changes") {
    val software: Column<String> = text("software")
            .references(Codes.software)
            .references(BugfixRevisions.software)
            .primaryKey()
    val id: Column<Int> = integer("id")
            .autoIncrement()
            .primaryKey()

    val filePath: Column<String> = text("filepath")
    val author: Column<String> = text("author")

    val beforeID: Column<Int?> = reference("beforeID", Codes.id)
            .nullable()
    val beforeHash: Column<Blob?> = blob("beforeHash")
            .nullable()

    val afterID: Column<Int?> = reference("afterID", Codes.id)
            .nullable()
    val afterHash: Column<Blob?> = blob("afterHash")
            .nullable()

    val revision: Column<String> = text("revision")
            .references(BugfixRevisions.id)
    val date: Column<String> = text("date")
    val changeType: Column<Int> = integer("changetype")
    val diffType: Column<Int> = integer("difftype")


    val beforeCodes: Alias<Codes> = Codes.alias("c1")
    val afterCodes: Alias<Codes> = Codes.alias("c2")

    override fun read(row: ResultRow): Change = Change(
            id = row[id],
            softwareName = row[software],
            filePath = Paths.get(row[filePath]),
            author = row[author],
            beforeCode = read(row, beforeCodes),
            afterCode = read(row, afterCodes),
            commitHash = row[revision],
            date = LocalDateTime.parse(row[date], ammoniaDateTimeFormatter),
            changeType = ChangeType.values().first { it.rawValue == row[changeType] },
            diffType = DiffType.values().first { it.rawValue == row[diffType] }
    )

    private fun read(row: ResultRow, alias: Alias<Codes>): Code {
        return Code(
                softwareName = row[alias[Codes.software]],
                rawText = row[alias[Codes.rText]],
                normalizedText = row[alias[Codes.nText]],
                range = lineRangeOf(
                        start = row[alias[Codes.start]],
                        stop = row[alias[Codes.end]]
                ),
                id = row[alias[Codes.id]]
        )
    }

    override fun insert(value: Change, insertId: Int?): InsertStatement<Number> = insert {
        val newID = insertId ?: value.id ?: throw IllegalStateException("Change has no ID: $value")
        value.id = newID
        it[id] = newID
        it[software] = value.softwareName
        it[filePath] = value.filePath.toString()
        it[author] = value.author
        value.beforeCode?.let { code ->
            it[beforeID] = code.id
            it[beforeHash] = code.hash.toBlob()
        }
        value.afterCode?.let { code ->
            it[afterID] = code.id
            it[afterHash] = code.hash.toBlob()
        }
        it[revision] = value.commitHash
        it[date] = value.date.format(ammoniaDateTimeFormatter)
        it[changeType] = value.changeType.rawValue
        it[diffType] = value.diffType.rawValue
    }
}