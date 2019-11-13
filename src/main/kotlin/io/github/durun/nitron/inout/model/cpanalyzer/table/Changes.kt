package io.github.durun.nitron.inout.model.cpanalyzer.table

import io.github.durun.nitron.inout.model.cpanalyzer.Change
import io.github.durun.nitron.inout.model.cpanalyzer.ChangeType
import io.github.durun.nitron.inout.model.cpanalyzer.DiffType
import io.github.durun.nitron.inout.model.cpanalyzer.ammoniaDateFormat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.nio.file.Paths
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob


object Changes : ReadWritableTable<Change>("changes") {
    val software: Column<String> = text("software")
            .references(Codes.software)
            .references(BugfixRevisions.software)
            .primaryKey()
    val id: Column<Long> = long("id")
            .autoIncrement()
            .primaryKey()

    val filePath: Column<String> = text("filepath")
    val author: Column<String> = text("author")

    val beforeID: Column<Long?> = reference("beforeID", Codes.id)
            .nullable()
    val beforeHash: Column<Blob?> = blob("beforeHash")
            .nullable()

    val afterID: Column<Long?> = reference("afterID", Codes.id)
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

    override fun read(row: ResultRow, alias: Alias<ReadableTable<Change>>?): Change {
        fun <T> Column<T>.get(): T = row[alias?.get(this) ?: this]
        fun Alias<Codes>.get() = Codes.read(row, this)
        return Change(
                id = id.get(),
                softwareName = software.get(),
                filePath = Paths.get(filePath.get()),
                author = author.get(),
                beforeCode = beforeCodes.get(),
                afterCode = afterCodes.get(),
                commitHash = revision.get(),
                date = ammoniaDateFormat.parse(date.get()),
                changeType = ChangeType.values().first { it.rawValue == changeType.get() },
                diffType = DiffType.values().first { it.rawValue == diffType.get() }
        )
    }

    override fun write(value: Change, insertId: Long?): InsertStatement<Number> = insert {
        val newID = insertId ?: value.id
        if (newID != null) it[id] = newID
        it[software] = value.softwareName
        it[filePath] = value.filePath.toString()
        it[author] = value.author
        value.beforeCode?.let { code ->
            it[beforeID] = code.id
            it[beforeHash] = SerialBlob(code.hash)
        }
        value.afterCode?.let { code ->
            it[afterID] = code.id
            it[afterHash] = SerialBlob(code.hash)
        }
        it[revision] = value.commitHash
        it[date] = ammoniaDateFormat.format(value.date)
        it[changeType] = value.changeType.rawValue
        it[diffType] = value.diffType.rawValue
    }
}