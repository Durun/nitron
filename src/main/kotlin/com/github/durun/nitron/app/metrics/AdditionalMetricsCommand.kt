package com.github.durun.nitron.app.metrics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import com.github.durun.nitron.core.toMD5
import com.github.durun.nitron.inout.database.SQLiteDatabase
import com.github.durun.nitron.inout.model.metrics.ChangesTable
import com.github.durun.nitron.inout.model.metrics.CodesTable
import com.github.durun.nitron.inout.model.metrics.GlobalPatternsTable
import com.github.durun.nitron.inout.model.metrics.MetricsTable
import com.github.durun.nitron.util.Log
import com.github.durun.nitron.util.LogLevel
import com.github.durun.nitron.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import kotlin.math.ln

class AdditionalMetricsCommand : CliktCommand(name = "additionalMetrics") {
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(mustBeReadable = true)
        .multiple()

    private val log by logger()

    override fun run() {
        LogLevel = Log.Level.VERBOSE
        dbFiles.forEach { dbFile ->
            val db = SQLiteDatabase.connect(dbFile)
            processOneDb(db)
            log.info { "Done: $dbFile" }
        }
    }

    private fun processOneDb(db: Database) {
        log.info { "Reading DB" }

        val metrices = transaction(db) {
            val c1 = CodesTable.alias("c1")
            val c2 = CodesTable.alias("c2")
            GlobalPatternsTable
                .innerJoin(c1, onColumn = { beforeHash }, otherColumn = { c1[CodesTable.hash] })
                .innerJoin(c2, onColumn = { GlobalPatternsTable.afterHash }, otherColumn = { c2[CodesTable.hash] })
                .selectAll()
                .mapIndexed { i, it ->
                    val blob = it[GlobalPatternsTable.beforeHash] to it[GlobalPatternsTable.afterHash]
                    val hash = blob.first?.toMD5() to blob.second?.toMD5()

                    if (i % 100000 == 0) log.info { "Read: $i" }

                    Metrics(
                        pattern = Pattern(
                            hash, blob, text = it[c1[CodesTable.text]] to it[c2[CodesTable.text]]
                        ),
                        support = it[GlobalPatternsTable.support],
                        confidence = it[GlobalPatternsTable.confidence],
                        projects = it[GlobalPatternsTable.projects],
                        files = it[GlobalPatternsTable.files],
                        authors = it[GlobalPatternsTable.authors],
                        bugfixWords = it[GlobalPatternsTable.bugfixWords],
                        testFiles = it[GlobalPatternsTable.testFiles]
                    )
                }
        }
        log.debug { "nPatterns: ${metrices.size}" }


        val softwares = transaction(db) {
            ChangesTable.slice(ChangesTable.software)
                .selectAll()
                .withDistinct(true)
                .asSequence()
                .map { it[ChangesTable.software] }
                .distinct()
                .toList()
        }
        log.debug { "Softwares: $softwares" }

        val nFiles = transaction(db) {
            ChangesTable.slice(ChangesTable.filepath)
                .selectAll()
                .withDistinct(true)
                .asSequence()
                .distinct()
                .count()
        }

        val additionalMetrices = runBlocking(Dispatchers.Default) {
            metrices.mapIndexed { i, metrics ->
                async {
                    val beforeText = metrics.pattern.text.first
                    val afterText = metrics.pattern.text.second
                    val beforeTokens = beforeText.split(' ')
                    val afterTokens = afterText.split(' ')


                    if (i % 100000 == 0) log.info { "Calc done: $i / ${metrices.size}" }

                    AdditionalMetrics(
                        pattern = metrics.pattern,
                        projectIdf = ln(softwares.size.toDouble() / metrics.projects),
                        fileIdf = ln(nFiles.toDouble() / metrics.files),
                        dChars = afterText.length - beforeText.length,
                        dTokens = afterTokens.size - beforeTokens.size,
                        changeToLessThan = countChangeToLessThan(beforeTokens, afterTokens),
                        styleOnly = isStyleOnlyChange(beforeText, afterText)
                    )
                }
            }.map { it.await() }
        }

        log.info { "Calc done." }
        log.info { "Writing to DB." }

        transaction(db) {
            SchemaUtils.drop(MetricsTable)
            SchemaUtils.createMissingTablesAndColumns(MetricsTable)
        }
        transaction(db) {
            MetricsTable.batchInsert(additionalMetrices, ignore = true) {
                this[MetricsTable.beforeHash] = it.pattern.blob.first
                this[MetricsTable.afterHash] = it.pattern.blob.second
                this[MetricsTable.projectIdf] = it.projectIdf
                this[MetricsTable.fileIdf] = it.fileIdf
                this[MetricsTable.dChars] = it.dChars
                this[MetricsTable.dTokens] = it.dTokens
                this[MetricsTable.changeToLessThan] = it.changeToLessThan
                this[MetricsTable.styleOnly] = if (it.styleOnly) 1 else 0
            }
        }
    }

    companion object {
        fun countChangeToLessThan(beforeTokens: List<String>, afterTokens: List<String>): Int {
            val beforeLt = beforeTokens.count { it == "<" || it == "<=" }
            val afterLt = afterTokens.count { it == "<" || it == "<=" }
            val beforeGt = beforeTokens.count { it == ">" || it == ">=" }
            val afterGt = afterTokens.count { it == ">" || it == ">=" }
            val dLessThan = afterLt - beforeLt
            val dGreaterThan = afterGt - beforeGt
            return if (dLessThan == -dGreaterThan) dLessThan else 0
        }

        fun isStyleOnlyChange(beforeText: String, afterText: String): Boolean {
            val (inner, outer) = when {
                beforeText.startsWith(afterText) -> afterText to beforeText
                afterText.startsWith(beforeText) -> beforeText to afterText
                else -> return false
            }
            val suffix = outer.drop(inner.length)
                .trim()
            return suffix == "{"
        }
    }
}