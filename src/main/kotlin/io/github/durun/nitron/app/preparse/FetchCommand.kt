package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.RepositoryTable
import io.github.durun.nitron.util.logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URL
import java.nio.file.Path

class FetchCommand : CliktCommand(name = "preparse-fetch") {

	private val customConfig: Path? by option("--config")
		.path(readable = true)
	private val config = (customConfig ?: Path.of("config/nitron.json"))
		.let { NitronConfigLoader.load(it) }
	private val workingDir: File by option("--dir")
		.file(folderOkay = true, fileOkay = false)
		.defaultLazy { Path.of("tmp").toFile() }
	private val dbFile: Path by argument(name = "DATABASE", help = "Database file")
		.path(writable = true)


	private val log by logger()

	override fun run() {
		val db = SQLiteDatabase.connect(dbFile)
		val repos: List<URL> = transaction(db) {
			RepositoryTable
				.slice(RepositoryTable.url)
				.selectAll()
				.map { URL(it[RepositoryTable.url]) }
		}
		log.info { "Fetch list:\n${repos.joinToString("\n").prependIndent("\t")}" }
		repos.forEach { repoUrl ->
			val repo = openRepository(repoUrl)
			val mainBranch = detectMainBranch(repo)
			log.info { "Detected main branch is: $mainBranch" }
		}
	}

	private fun openRepository(repoUrl: URL): Git {
		val repoDir = workingDir.resolve(repoUrl.file.trim('/'))
		return runCatching {
			log.info { "Opening: $repoUrl" }
			Git.open(repoDir)
		}.recover {
			log.info { "Cloning: $repoUrl" }
			Git.cloneRepository()
				.setDirectory(repoDir)
				.setURI(repoUrl.toString())
				.call()
		}.getOrThrow()
	}

	private fun detectMainBranch(repo: Git): Ref {
		val branches: List<Ref> = repo.branchList()
			.setListMode(ListBranchCommand.ListMode.REMOTE)
			.setContains("main")
			.setContains("master")
			.call()
		return branches.firstOrNull()
			?: throw Exception("No main branch")
	}
}

