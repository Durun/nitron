package io.github.durun.nitron.inout.model.table

import io.github.durun.nitron.core.toMD5
import io.github.durun.nitron.inout.model.ChangeType
import io.github.durun.nitron.inout.model.DiffType
import io.github.durun.nitron.inout.model.Pattern
import io.github.durun.nitron.inout.model.ammoniaDateTimeFormatter
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import java.sql.Blob
import java.time.LocalDateTime


object Patterns : ReadableTable<Pattern>("patterns") {
    val id: Column<Int> = integer("id")
        .autoIncrement()
        .primaryKey()
    val beforeHash: Column<Blob?> = blob("beforeHash").nullable()
    val afterHash: Column<Blob?> = blob("afterHash").nullable()
    val changeType: Column<Int> = integer("changetype")
    val diffType: Column<Int> = integer("difftype")
    val support: Column<Int> = integer("support")
    val confidence: Column<Double> = double("confidence")
    val authors: Column<Int> = integer("authors")
    val files: Column<Int> = integer("files")
    val nos: Column<Int> = integer("nos")
    val firstDate: Column<String> = text("firstDate")
    val lastDate: Column<String> = text("lastDate")

    override fun read(row: ResultRow): Pattern {
        val changeType = ChangeType.values().first { it.rawValue == row[changeType] }
        val hash = when (changeType) {
            ChangeType.CHANGE -> {
                val before = row[beforeHash]?.toMD5() ?: throw IllegalStateException("$row has no beforeCode column")
                val after = row[afterHash]?.toMD5() ?: throw IllegalStateException("$row has no afterCode column")
                before to after
            }
            ChangeType.ADD -> {
                val after = row[afterHash]?.toMD5() ?: throw IllegalStateException("$row has no afterCode column")
                null to after
            }
            ChangeType.DELETE -> {
                val before = row[beforeHash]?.toMD5() ?: throw IllegalStateException("$row has no beforeCode column")
                before to null
            }
        }
        return Pattern(
            id = row[id],
            beforeHash = hash.first,
            afterHash = hash.second,
            changeType = changeType,
            diffType = DiffType.values().first { it.rawValue == row[diffType] },
            support = row[support],
            confidence = row[confidence],
            authors = row[authors],
            files = row[files],
            nos = row[nos],
            dateRange = row[firstDate].parseToDate()..row[lastDate].parseToDate()
        )
    }

    private fun String.parseToDate() = LocalDateTime.parse(this, ammoniaDateTimeFormatter)
}