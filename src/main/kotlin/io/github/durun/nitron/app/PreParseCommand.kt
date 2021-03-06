package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.RepositoryTable
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.nio.file.Path

class PreParseCommand : CliktCommand(name = "preparse") {
	private val dbFile: Path by argument(name = "database", help = "Database file")
		.path(writable = true)
	private val urls: List<URL> by option("--url", help = "Git repository URL")
		.convert { URL(it) }
		.multiple()
		.validate {
			// TODO: Validate URL
		}

	private val log by logger()

	override fun run() {
		val db = SQLiteDatabase.connect(dbFile)
		transaction(db) {
			log.debug { "Opened $dbFile" }
			SchemaUtils.createMissingTablesAndColumns(RepositoryTable)
			urls.forEach { newUrl ->
				RepositoryTable.insertIgnoreAndGetId {
					it[url] = newUrl.toString()
					it[name] = newUrl.file.trim('/')
				}
					?.let { log.debug { "Wrote: $newUrl" } }
					?: run { log.info { "Already exists: $newUrl" } }
			}
		}
		log.debug { "Closed $dbFile" }
	}
}