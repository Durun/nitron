package io.github.durun.nitron.inout.model.metrics

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.sql.Blob

object CodesTable : Table("codes") {
    val hash: Column<Blob> = blob("hash")
    val text: Column<String> = text("text")
}

object RevisionsTable : Table("revisions") {
    val software: Column<String> = text("software")
    val id: Column<String> = text("id")
    val date: Column<String> = text("date")
    val message: Column<String> = text("message")
    val author: Column<String> = text("author")
}

object ChangesTable : Table("changes") {
    val software: Column<String> = text("software")
    val filepath: Column<String> = text("filepath")
    val beforeHash: Column<Blob?> = blob("beforeHash").nullable()
    val afterHash: Column<Blob?> = blob("afterHash").nullable()
    val revision: Column<String> = text("revision")
}

object GlobalPatternsTable : Table("globalPatterns") {
    // id
    val beforeHash: Column<Blob?> = blob("beforeHash").nullable()
    val afterHash: Column<Blob?> = blob("afterHash").nullable()

    // metrics
    val support: Column<Int> = integer("support")
    val confidence: Column<Double> = double("confidence")
    // TODO: more metrics
}