package com.github.durun.nitron.app.metrics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import com.github.durun.nitron.core.MD5
import com.github.durun.nitron.core.toMD5
import com.github.durun.nitron.inout.database.SQLiteDatabase
import com.github.durun.nitron.inout.model.metrics.ChangesTable
import com.github.durun.nitron.inout.model.metrics.CodesTable
import com.github.durun.nitron.inout.model.metrics.GlobalPatternsTable
import com.github.durun.nitron.inout.model.metrics.RevisionsTable
import com.github.durun.nitron.util.Log
import com.github.durun.nitron.util.LogLevel
import com.github.durun.nitron.util.logger
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
        val patterns: MutableMap<Pair<MD5?, MD5?>, Pattern> = mutableMapOf()
        val revisions: MutableMap<String, Revision> = mutableMapOf()

        val changes = transaction(db) {
            val c1 = CodesTable.alias("c1")
            val c2 = CodesTable.alias("c2")
            ChangesTable
                .innerJoin(c1, onColumn = { beforeHash }, otherColumn = { c1[CodesTable.hash] })
                .innerJoin(c2, onColumn = { ChangesTable.afterHash }, otherColumn = { c2[CodesTable.hash] })
                .innerJoin(RevisionsTable, onColumn = { ChangesTable.revision }, otherColumn = { id })
                .selectAll()
                .map {
                    val hashPair = it[ChangesTable.beforeHash]?.toMD5() to it[ChangesTable.afterHash]?.toMD5()
                    val revisionId = it[ChangesTable.revision]
                    Change(
                        software = it[ChangesTable.software],
                        filePath = it[ChangesTable.filepath],
                        pattern = patterns.computeIfAbsent(hashPair) { _ ->
                            Pattern(
                                hash = hashPair,
                                blob = it[ChangesTable.beforeHash] to it[ChangesTable.afterHash],
                                text = it[c1[CodesTable.text]] to it[c2[CodesTable.text]]
                            )
                        },
                        revision = revisions.computeIfAbsent(revisionId) { _ ->
                            Revision(
                                id = revisionId,
                                author = runCatching { it[RevisionsTable.author] }.getOrDefault("null"),
                                message = runCatching { it[RevisionsTable.message] }.getOrDefault("null")
                            )
                        }
                    )

                }
        }

        //val patterns = changes.map { it.pattern }.distinct()
        log.debug { "nPatterns: ${patterns.size}" }

        val softwares = changes.groupBy { it.software }
        val nDocuments = softwares.size
        val nFiles = changes.distinctBy { it.filePath }.count()
        log.debug { "Softwares: ${softwares.keys}" }


        val metricses = runBlocking(Dispatchers.Default) {
            patterns.values.mapIndexed { i, pattern ->
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

                    // count test file
                    val testFiles = supportingChanges.count { change ->
                        isTestPath(change.filePath)
                    }

                    val beforeText = pattern.text.first
                    val afterText = pattern.text.second
                    val beforeTokens = beforeText.split(' ')
                    val afterTokens = afterText.split(' ')

                    // count operators
                    val beforeLt = beforeTokens.count { it == "<" || it == "<=" }
                    val afterLt = afterTokens.count { it == "<" || it == "<=" }
                    val beforeGt = beforeTokens.count { it == ">" || it == ">=" }
                    val afterGt = afterTokens.count { it == ">" || it == ">=" }
                    val dLessThan = afterLt - beforeLt
                    val dGreaterThan = afterGt - beforeGt

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
                        dTokens = afterTokens.size - beforeTokens.size,
                        authors = supportingChanges.distinctBy { it.revision.author }.count(),
                        bugfixWords = bugfixWords.toDouble() / sup,
                        testFiles = testFiles.toDouble() / sup,
                        changeToLessThan = if (dLessThan == -dGreaterThan) dLessThan else 0
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
                    it[testFiles] = metrics.testFiles
                    it[changeToLessThan] = metrics.changeToLessThan
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

        private fun isTestPath(path: String): Boolean {
            if (path.contains("Test")) return true

            val names = path.split('/')
            val fileName = names.lastOrNull()?.split('.')?.firstOrNull() ?: ""
            val dirNames = names.dropLast(1)

            if (fileName.startsWith("test")) return true
            if (dirNames.any { it.startsWith("test") }) return true
            return false
        }
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
    val testFiles: Double,  // テストコードであるchangesの数
    val changeToLessThan: Int,  // >, >= から <, <= への変更がされた数
)