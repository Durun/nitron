package io.github.durun.nitron.app.metrics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.metrics.ChangesTable
import io.github.durun.nitron.inout.model.metrics.GlobalPatternsTable
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path

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
        val patterns = transaction(db) {
            ChangesTable.slice(ChangesTable.beforeHash, ChangesTable.afterHash)
                .selectAll().withDistinct()
                .map { it[ChangesTable.beforeHash] to it[ChangesTable.afterHash] }
        }
        transaction(db) {
            patterns.forEach { (beforeBlob, afterBlob) ->
                GlobalPatternsTable.insert {
                    it[beforeHash] = beforeBlob
                    it[afterHash] = afterBlob
                    it[support] = 0         // TODO
                    it[confidence] = 0.0    // TODO
                }
            }
        }
    }
}