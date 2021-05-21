package io.github.durun.nitron.app.metrics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.toMD5
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.metrics.ChangesTable
import io.github.durun.nitron.inout.model.metrics.CodesTable
import io.github.durun.nitron.inout.model.metrics.GlobalPatternsTable
import io.github.durun.nitron.inout.model.metrics.RevisionsTable
import io.github.durun.nitron.util.Log
import io.github.durun.nitron.util.LogLevel
import io.github.durun.nitron.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.sql.Blob
import kotlin.math.ln

class MetricsCommand : CliktCommand(name = "metrics") {
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(readable = true)
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
        val changes = transaction(db) {
            val c1 = CodesTable.alias("c1")
            val c2 = CodesTable.alias("c2")
            ChangesTable
                .innerJoin(c1, onColumn = { beforeHash }, otherColumn = { c1[CodesTable.hash] })
                .innerJoin(c2, onColumn = { ChangesTable.afterHash }, otherColumn = { c2[CodesTable.hash] })
                .innerJoin(RevisionsTable, onColumn = { ChangesTable.revision }, otherColumn = { id })
                .selectAll()
                .map {
                    Change(
                        software = it[ChangesTable.software],
                        filePath = it[ChangesTable.filepath],
                        pattern = Pattern(
                            hash = it[ChangesTable.beforeHash]?.toMD5() to it[ChangesTable.afterHash]?.toMD5(),
                            blob = it[ChangesTable.beforeHash] to it[ChangesTable.afterHash],
                            text = it[c1[CodesTable.text]] to it[c2[CodesTable.text]]
                        ),
                        revision = Revision(
                            id = it[ChangesTable.revision],
                            author = it[RevisionsTable.author],
                            message = it[RevisionsTable.message]
                        )
                    )

                }
        }

        val patterns = changes.map { it.pattern }.distinct()
        log.debug { "nPatterns: ${patterns.size}" }

        val softwares = changes.groupBy { it.software }
        val nDocuments = softwares.size
        val nFiles = changes.distinctBy { it.filePath }.count()
        log.debug { "Softwares: ${softwares.keys}" }


        val metricses = runBlocking(Dispatchers.Default) {
            patterns.mapIndexed { i, pattern ->
                async {
                    val supportingChanges = changes.filter { it.pattern.hash == pattern.hash }
                    val sup = supportingChanges.count()
                    val left = changes.count { it.pattern.hash.first == pattern.hash.first }

                    // idf
                    val projects = softwares.count { (_, changeList) ->
                        changeList.any { it.pattern.hash == pattern.hash }
                    }
                    val files = supportingChanges
                        .distinctBy { it.filePath }
                        .count()

                    // bugfix words
                    val bugfixWords = supportingChanges.count { change ->
                        bugfixKeywords.any { change.revision.message.contains(it, ignoreCase = true) }
                    }

                    val beforeText = pattern.text.first
                    val afterText = pattern.text.second

                    if (i % 1000 == 0) log.info { "Calc done: $i / ${patterns.size}" }

                    Metrics(
                        pattern,
                        support = sup,
                        confidence = sup.toDouble() / left,
                        projects = projects,
                        projectIdf = ln(nDocuments.toDouble() / projects),
                        files = files,
                        fileIdf = ln(nFiles.toDouble() / files),
                        dChars = afterText.length - beforeText.length,
                        dTokens = afterText.split(' ').size - beforeText.split(' ').size,
                        authors = supportingChanges.distinctBy { it.revision.author }.count(),
                        bugfixWords = bugfixWords.toDouble() / sup
                    )
                }
            }.map { it.await() }
        }

        transaction(db) {
            SchemaUtils.drop(GlobalPatternsTable)
            SchemaUtils.createMissingTablesAndColumns(GlobalPatternsTable)
        }
        transaction(db) {
            metricses.forEach { metrics ->
                GlobalPatternsTable.insert {
                    it[beforeHash] = metrics.pattern.blob.first
                    it[afterHash] = metrics.pattern.blob.second
                    it[support] = metrics.support
                    it[confidence] = metrics.confidence
                    it[projects] = metrics.projects
                    it[projectIdf] = metrics.projectIdf
                    it[files] = metrics.files
                    it[fileIdf] = metrics.fileIdf
                    it[dChars] = metrics.dChars
                    it[dTokens] = metrics.dTokens
                    it[authors] = metrics.authors
                    it[bugfixWords] = metrics.bugfixWords
                }
            }
        }
    }

    companion object {
        private val bugfixKeywords = listOf(
            "fix",
            "bug",
            "error",
            "fault",
            "issue",
            "mistake",
            "incorrect",
            "defect",
            "flaw"
        )
    }
}

private data class Revision(
    val id: String,
    val author: String,
    val message: String
)

private data class Change(
    val software: String,
    val filePath: String,
    val pattern: Pattern,
    val revision: Revision
)

private data class Pattern(
    val hash: Pair<MD5?, MD5?>,
    val blob: Pair<Blob?, Blob?>,
    val text: Pair<String, String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pattern

        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }
}

private data class Metrics(
    val pattern: Pattern,
    val support: Int,
    val confidence: Double,
    val projects: Int,
    val projectIdf: Double,
    val files: Int,
    val fileIdf: Double,
    val dChars: Int,    // Pattern前後の文字数の増減
    val dTokens: Int,   // Pattern前後のトークン数の増減
    val authors: Int,
    val bugfixWords: Double,// コミットメッセージに bugfix keyword が含まれているchangesの数
)