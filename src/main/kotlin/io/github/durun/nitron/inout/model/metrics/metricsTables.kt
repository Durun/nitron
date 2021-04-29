package io.github.durun.nitron.inout.model.metrics

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.joda.time.DateTime
import java.sql.Blob

object PatternMetricsTable : IntIdTable("pattern_metrics") {
    // id
    val beforeHash: Column<Blob?> = blob("beforeHash").nullable()
    val afterHash: Column<Blob?> = blob("afterHash").nullable()

    // content
    val beforeNText: Column<String?> = text("beforeNText").nullable()
    val afterNText: Column<String?> = text("afterNText").nullable()

    // metrics
    val support: Column<Int> = integer("support")
    val confidence: Column<Double> = double("confidence")
    val authors: Column<Int> = integer("authors")
    val files: Column<Int> = integer("files")
    val firstDate: Column<DateTime> = datetime("firstDate")
    val lastDate: Column<DateTime> = datetime("firstDate")
}