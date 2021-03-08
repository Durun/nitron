package io.github.durun.nitron.inout.model.preparse

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.inout.database.MemoryDatabase
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.lib.ObjectId
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
class AstTableTest : FreeSpec({
    "table" {
        val config = NitronConfigLoader.load(Path.of("config/nitron.json"))
        val db = MemoryDatabase.connect("AstTableTest-table")

        transaction(db) {
            transaction {
                SchemaUtils.createMissingTablesAndColumns(
                    RepositoryTable,
                    CommitTable,
                    FileTable,
                    AstTable,
                    AstContentTable
                )
            }
            val repositoryId = transaction {
                RepositoryTable.insertAndGetId(
                    url = URL("https://github.com/example/testProject.git"),
                    langs = listOf("java", "kotlin")
                )
            }
            val commitId = transaction {
                CommitTable.insertAndGetId(
                    repositoryId,
                    hash = "1234abcd1234abcd1234abcd1234abcd1234abcd",
                    message = "test commit",
                    date = DateTime(12345),
                    author = "Test Author"
                )
            }
            val fileId = transaction {
                FileTable.insertAndGetId(
                    commitId,
                    path = "path/to/file.java",
                    objectId = ObjectId.fromString("abcd1234abcd1234abcd1234abcd1234abcd1234"),
                    checksum = MD5.digest("file content")
                )
            }
            val langId = transaction {
                LanguageTable.insertAndGetId("java", config.langConfig["java"]!!)
            }
            val astId = transaction {
                AstTable.insertAndGetId(fileId,langId, null)
            }
            val contentId = transaction {
                AstContentTable.insertAndGetId("Test content")
            }
            transaction {
                AstTable.selectAll().first().let {
                    it[AstTable.content] shouldBe null
                }
            }
            transaction {
                AstTable.updateContent(astId, contentId)
            }
            transaction {
                AstTable.selectAll().first().let {
                    it[AstTable.content] shouldBe contentId
                }
            }
            transaction {
                AstContentTable.deleteWhere { AstContentTable.id eq contentId }
                AstTable.setNullOnAbsentContent()
            }
            transaction {
                AstTable.selectAll().first().let {
                    it[AstTable.content] shouldBe null
                }
            }
        }
    }
})
