package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.config.NitronConfig
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
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

	private val customConfig: Path? by option("--config")
		.path(readable = true)
	private val config = (customConfig ?: Path.of("config/nitron.json"))
		.let { NitronConfigLoader.load(it) }
	private val dbFile: Path by argument(name = "--database", help = "Database file")
		.path(folderOkay = false)
	private val remote: URL? by option("--remote", help = "Git repository URL")
		.convert {
			val gitUrl = if (it.endsWith(".git")) it else "$it.git"
			URL(gitUrl)
		}
		.validate(gitUrlValidator)
	private val langArgs: List<String> by option("--lang", help = "Language config name")
		.multiple()
		.validate(langsValidator(config))


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
			it[langs] = langArgs.joinToString(",")
		}
			?.let { log.debug { "Wrote: $remoteUrl" } }
			?: run { log.info { "Already exists: $remoteUrl" } }
	}
}

private val gitUrlValidator: OptionValidator<URL> = { url ->
	require(url.toString().endsWith(".git")) { "Must end with '.git' but: $url" }
	runCatching {
		Git.lsRemoteRepository()
			.setRemote(url.toString())
			.call()
	}.onFailure {
		require(false) { "Invalid remote: ${it.localizedMessage}" }
	}
}

private fun langsValidator(config: NitronConfig): OptionValidator<Collection<String>> = { inputs ->
	require(inputs.isNotEmpty()) { "No languages specified" }
	val languages = config.langConfig.keys
	val errors = inputs.filterNot { languages.contains(it) }
	require(errors.isEmpty()) { "Language $errors not found in $languages" }
}