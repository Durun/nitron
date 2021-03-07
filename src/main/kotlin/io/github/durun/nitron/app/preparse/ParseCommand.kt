package io.github.durun.nitron.app.preparse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.preparse.AstContentTable
import io.github.durun.nitron.inout.model.preparse.AstTable
import io.github.durun.nitron.inout.model.preparse.RepositoryTable
import io.github.durun.nitron.util.logger
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.net.URL
import java.nio.file.Path

class ParseCommand : CliktCommand(name = "preparse") {

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

    @kotlin.io.path.ExperimentalPathApi
    override fun run() {
        val db = SQLiteDatabase.connect(dbFile)
        val dbUtil = DbUtil(db)
        val gitUtil = GitUtil(workingDir)

        log.info { "Available languages: ${config.langConfig.keys}" }
        config.langConfig.entries.forEach { (name, config) ->
            check(dbUtil.isLanguageConsistent(name, config)) { "Invalid language: $name" }
            log.verbose { "Language check OK: $name" }
        }

        // list asts table
        dbUtil.prepareAstTable(config)

        val (repoId, repoUrl) = transaction(db) {
            RepositoryTable
                .selectAll()
                .map { it[RepositoryTable.id] to it[RepositoryTable.url] }
                .last()
        }

        val git = gitUtil.openRepository(URL(repoUrl))
        val parseUtil = ParseUtil(git, config)

        transaction(db) {
            val (astId, objectId, lang) = dbUtil.queryAbsentAst(repoId).first()

            val parsed = parseUtil.parseText(parseUtil.readFile(objectId)!!, lang, config.langConfig[lang]!!)

            val contentId = AstContentTable.insertIgnoreAndGetId {
                it[content] = parsed
                it[checksum] = MD5.digest(parsed).toString()
            }
            AstTable.update({ AstTable.id eq astId }) {
                it[content] = contentId
            }
        }

    }
}