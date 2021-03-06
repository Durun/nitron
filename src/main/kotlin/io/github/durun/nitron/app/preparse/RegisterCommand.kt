package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.OptionValidator
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.RepositoryTable
import io.github.durun.nitron.util.logger
import org.eclipse.jgit.api.Git
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.nio.file.Path

class RegisterCommand : CliktCommand(name = "preparse-register") {

	private val dbFile: Path by argument(name = "database", help = "Database file")
		.path(writable = true)
	private val remote: URL? by option("--remote", help = "Git repository URL")
		.convert { URL(it) }
		.validate(gitUrlValidator)

	private val log by logger()

	override fun run() {
		val db = SQLiteDatabase.connect(dbFile)
		remote?.registerRemoteRepository(db)
	}

	private fun URL.registerRemoteRepository(db: Database) = transaction(db) {
		val remoteUrl = this@registerRemoteRepository
		SchemaUtils.createMissingTablesAndColumns(RepositoryTable)
		RepositoryTable.insertIgnoreAndGetId {
			it[url] = remoteUrl.toString()
			it[name] = remoteUrl.file.trim('/')
		}
			?.let { log.debug { "Wrote: $remoteUrl" } }
			?: run { log.info { "Already exists: $remoteUrl" } }
	}

}

private val gitUrlValidator: OptionValidator<URL> = { url ->
	runCatching {
		Git.lsRemoteRepository()
			.setRemote(url.toString())
			.call()
	}.onFailure {
		require(false) { "Invalid remote: ${it.localizedMessage}" }
	}
}