package com.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import com.github.durun.nitron.core.config.NitronConfig
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import com.github.durun.nitron.inout.database.SQLiteDatabase
import com.github.durun.nitron.inout.model.preparse.RepositoryTable
import com.github.durun.nitron.inout.model.preparse.insertIgnoreAndGetId
import com.github.durun.nitron.util.logger
import org.eclipse.jgit.api.Git
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.nio.file.Path

class RegisterCommand : CliktCommand(name = "preparse-register") {

	private val customConfig: Path? by option("--config")
        .path(mustBeReadable = true)
	private val config = (customConfig ?: Path.of("config/nitron.json"))
		.let { NitronConfigLoader.load(it) }
	private val dbFile: Path by argument(name = "DATABASE", help = "Database file")
        .path(canBeDir = false)
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
		RepositoryTable.insertIgnoreAndGetId(
			url = remoteUrl,
			langs = langArgs
		)
			?.let { log.debug { "Wrote: $remoteUrl" } }
			?: run { log.info { "Already exists: $remoteUrl" } }
	}
}

val gitUrlValidator: OptionValidator<URL> = { url ->
	require(url.toString().endsWith(".git")) { "Must end with '.git' but: $url" }
	runCatching {
		Git.lsRemoteRepository()
			.setRemote(url.toString())
			.call()
	}.onFailure {
		require(false) { "Invalid remote: ${it.localizedMessage}" }
	}
}

fun langsValidator(config: NitronConfig): OptionValidator<Collection<String>> = { inputs ->
	require(inputs.isNotEmpty()) { "No languages specified" }
	val languages = config.langConfig.keys
	val errors = inputs.filterNot { languages.contains(it) }
	require(errors.isEmpty()) { "Language $errors not found in $languages" }
}