package io.github.durun.nitron.inout.model.table

import io.github.durun.nitron.core.ast.basic.lineRangeOf
import io.github.durun.nitron.inout.model.Code
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement


object Codes : ReadWritableTable<Code>("codes") {
    val software: Column<String> = text("software").primaryKey()
    val id: Column<Int> = integer("id").autoIncrement().primaryKey()
    val rText: Column<String> = text("rText")
    val nText: Column<String> = text("nText")
    val hash: Column<String> = text("hash")
    val start: Column<Int> = integer("start")
    val end: Column<Int> = integer("end")

    override fun read(row: ResultRow): Code = Code(
            softwareName = row[software],
            id = row[id],
            rawText = row[rText],
            normalizedText = row[nText],
            range = lineRangeOf(
                    start = row[start],
                    stop = row[end]
            )

    )

    override fun insert(value: Code, insertId: Int?): InsertStatement<Number> = insert {
        val newID = insertId ?: value.id
        if (newID != null) it[id] = newID
        it[software] = value.softwareName
        it[rText] = value.rawText
        it[nText] = value.normalizedText
        it[hash] = value.hashString
        it[start] = value.range.line.start
        it[end] = value.range.line.stop
    }
}