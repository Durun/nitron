package io.github.durun.nitron.inout.model.cpanalyzer.table

import io.github.durun.nitron.core.ast.node.lineRangeOf
import io.github.durun.nitron.inout.model.ast.table.Structures
import io.github.durun.nitron.inout.model.cpanalyzer.Code
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.nio.ByteBuffer
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob


object Codes : ReadWritableTable<Code>("codes") {
    val software: Column<String> = text("software").primaryKey()
    val id: Column<Long> = long("id").autoIncrement().primaryKey()
    val rText: Column<String> = text("rText")
    val nText: Column<String> = text("nText")
    val hash: Column<Blob> = blob("hash")
    val start: Column<Int> = integer("start")
    val end: Column<Int> = integer("end")

    val structureIdMsb = optReference("structure_id_msb", Structures.idMsb)
    val structureIdLsb = optReference("structure_id_lsb", Structures.idLsb)

    override fun read(row: ResultRow, alias: Alias<ReadableTable<Code>>?): Code {
        fun <T> Column<T>.get(): T = row[alias?.get(this) ?: this]
        return Code(
                softwareName = software.get(),
                id = id.get(),
                rawText = rText.get(),
                normalizedText = nText.get(),
                range = lineRangeOf(
                        start = start.get(),
                        stop = end.get()
                ),
                structure = if (alias == null) Structures.read(row).node else null  // TODO
        )
    }

    override fun write(value: Code, insertId: Long?): InsertStatement<Number> = insert {
        val newID = insertId ?: value.id
        if (newID != null) it[id] = newID
        it[software] = value.softwareName
        it[rText] = value.rawText
        it[nText] = value.normalizedText
        it[start] = value.range.line.start
        it[end] = value.range.line.stop

        val bytes = value.hash
        it[hash] = SerialBlob(bytes)
        it[structureIdMsb] = ByteBuffer.wrap(bytes, 0, 8).long
        it[structureIdLsb] = ByteBuffer.wrap(bytes, 8, 8).long
    }
}