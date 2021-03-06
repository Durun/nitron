package io.github.durun.nitron.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.RepositoryTable
import io.github.durun.nitron.util.logger
import org.eclipse.jgit.api.Git
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
		.validate(gitUrlValidator)

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

private val gitUrlValidator: OptionValidator<List<URL>> = { urls ->
	val results = urls.map { url ->
		runCatching {
			val refs = Git.lsRemoteRepository()
				.setRemote(url.toString())
				.call()
			refs.size
		}
	}
	val errors = results.mapNotNull { it.exceptionOrNull() }
	require(errors.isEmpty()) {
		"Invalid remotes:\n${errors.joinToString("\n") { it.localizedMessage }.prependIndent("\t")}"
	}
}