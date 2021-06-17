package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.MD5
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.core.parser.antlr.AstBuildVisitor
import io.github.durun.nitron.core.parser.antlr.ParserStore
import io.github.durun.nitron.core.parser.antlr.nodeTypePoolOf
import io.github.durun.nitron.inout.database.MemoryDatabase
import io.github.durun.nitron.inout.model.preparse.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.lib.ObjectId
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi

@OptIn(ExperimentalPathApi::class)
class ExtractorTest : FreeSpec({
    "test" {
        val tree = parser.parse(testCode.reader(), javaConfig.startRule)
        val ast = tree.accept(converter)
        println(ast)

        val types = nodeTypePoolOf("java", parser.antlrParser)
        val parseUtil = ParseUtil(config)
        val repoUrl = URL("https://github.com/example/testProject.git")
        val commitHash = "1234abcd1234abcd1234abcd1234abcd1234abcd"
        val filePath = "path/to/file.java"
        val db = MemoryDatabase.connectNew()
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
                    url = repoUrl,
                    langs = listOf("java", "kotlin")
                )
            }
            val commitId = transaction {
                CommitTable.insertAndGetId(
                    repositoryId,
                    hash = commitHash,
                    message = "test commit",
                    date = DateTime(12345),
                    author = "Test Author"
                )
            }
            val fileId = transaction {
                FileTable.insertAndGetId(
                    commitId,
                    path = filePath,
                    objectId = ObjectId.fromString("abcd1234abcd1234abcd1234abcd1234abcd1234"),
                    checksum = MD5.digest(testCode)
                )
            }
            val langId = transaction {
                LanguageTable.insertAndGetId("java", config.langConfig["java"]!!)
            }
            val contentId = transaction {
                val parsed = parseUtil.parseText(testCode, "java", config.langConfig["java"]!!)
                AstContentTable.insertIfAbsentAndGetId(parsed)
            }
            transaction {
                AstTable.insertAndGetId(fileId,langId, contentId)
            }

            val extractor = Extractor(config,db)
            val ast1 = extractor.getAst(repoUrl.file.trim('/'), commitHash, filePath, "java", types)
            val ast2 = extractor.getAst(MD5.digest(testCode).toString(),"java", types)
            println(ast1)
            println(ast2)
            ast1 shouldBe ast
            ast2 shouldBe ast
        }
    }
})

private val config = NitronConfigLoader.load(Path.of("config/nitron.json"))
private val javaConfig = config.langConfig["java"]!!.grammar
private val parser = ParserStore.getOrThrow(javaConfig)
private val converter = AstBuildVisitor(parser.antlrParser.grammarFileName, parser.antlrParser)
private val testCode = """
    class Sample {
        public static void main(String[] args){
            String str = args[0];
            System.out.println("Hello, world!");
        }
    }
""".trimIndent()