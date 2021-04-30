package io.github.durun.nitron.app.metrics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.toMD5
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.metrics.ChangesTable
import io.github.durun.nitron.inout.model.metrics.GlobalPatternsTable
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.sql.Blob

class MetricsCommand : CliktCommand(name = "metrics") {
    private val dbFiles: List<Path> by argument(name = "DATABASE", help = "Database file")
        .path(readable = true)
        .multiple()

    private val log by logger()

    override fun run() {
        dbFiles.forEach { dbFile ->
            val db = SQLiteDatabase.connect(dbFile)
            processOneDb(db)
            log.info { "Done: $dbFile" }
        }
    }

    private fun processOneDb(db: Database) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(GlobalPatternsTable)
        }
        val changes = transaction(db) {
            ChangesTable
                .selectAll()
                .map {
                    Change(
                        software = it[ChangesTable.software],
                        filePath = it[ChangesTable.filepath],
                        pattern = Pattern(
                            hash = it[ChangesTable.beforeHash]?.toMD5() to it[ChangesTable.afterHash]?.toMD5(),
                            blob = it[ChangesTable.beforeHash] to it[ChangesTable.afterHash]
                        )
                    )

                }
        }
        transaction(db) {
            GlobalPatternsTable.deleteAll()
        }
        val patterns = changes.map { it.pattern }.distinct()
        transaction(db) {
            patterns.forEachIndexed { i, (hash, blob) ->
                val sup = changes.count { it.pattern.hash.first == hash.first }
                GlobalPatternsTable.insert {
                    it[beforeHash] = blob.first
                    it[afterHash] = blob.second
                    it[support] = sup
                    it[confidence] = 0.0    // TODO
                }
                if (i % 1000 == 0) log.info { "Done: $i / ${patterns.size}" }
            }
        }
    }
}

private data class Change(
    val software: String,
    val filePath: String,
    val pattern: Pattern
)

private data class Pattern(
    val hash: Pair<MD5?, MD5?>,
    val blob: Pair<Blob?, Blob?>
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