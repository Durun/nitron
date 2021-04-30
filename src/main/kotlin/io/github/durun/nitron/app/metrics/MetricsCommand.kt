package io.github.durun.nitron.app.metrics

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.sql.Database
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
        TODO()
    }
}