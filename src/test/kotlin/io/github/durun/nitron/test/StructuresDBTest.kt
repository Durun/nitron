package io.github.durun.nitron.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import io.github.durun.nitron.binding.cpanalyzer.JsonCodeRecorder
import io.github.durun.nitron.core.config.GrammarConfig
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.github.durun.nitron.core.decodeByteArray
import io.github.durun.nitron.core.encodeByteArray
import io.github.durun.nitron.core.parser.CommonParser
import io.github.durun.nitron.core.toHash
import io.github.durun.nitron.inout.database.SQLiteDatabase
import io.github.durun.nitron.inout.model.ast.NodeTypeSet
import io.github.durun.nitron.inout.model.ast.SerializableAst
import io.github.durun.nitron.inout.model.ast.table.*
import io.github.durun.nitron.inout.model.ast.toSerializable
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import org.antlr.v4.runtime.Parser
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths

class StructuresDBTest : FreeSpec() {
    private val path = Paths.get("testdata/database/test.db")
    private val langPath = Paths.get("config/lang/java.json")
    private val langConfig: LangConfig
    private val processor: CodeProcessor
    private val nodeTypeSet: NodeTypeSet
    private val db: Database

    init {
        db = SQLiteDatabase.connect(path)
        langConfig = LangConfigLoader.load(langPath)
        processor = CodeProcessor(langConfig, outputPath = path.parent.resolve("test.structures"))
        val antlrParser = langConfig.grammar.getParser()    // TODO
        nodeTypeSet = NodeTypeSet(grammarName = langConfig.fileName, parser = antlrParser)

        "prepare" - {
            "Structure is serializable" {
                val value = javaCode
                        .let { processor.parse(it) }
                        .toSerializable(nodeTypeSet)
                value.shouldNotBeNull()
            }
        }

        "a Structure is writable and readable" {
            val value = javaCode
                    .let { processor.parse(it) }
                    .toSerializable(nodeTypeSet)

            // init table
            transaction(db) {
                SchemaUtils.drop(Structures)
                SchemaUtils.drop(NodeTypeSets)
                SchemaUtils.create(Structures)
                SchemaUtils.create(NodeTypeSets)
            }

            // write
            val writer = StructuresWriter(db)
            println("writing: $value")
            writer.write(value)
            println("wrote: $value")

            // read
            val reader = StructuresReader(db)
            val readValue = reader.read().firstOrNull() ?: throw Exception("StructuresReader read nothing.")
            println("read: $readValue")
            readValue shouldBe value
        }

        "many Structure are writable and readable" {
            val value = javaCode
                    .let { processor.parse(it) }
                    .toSerializable(nodeTypeSet)
            val n = 100
            val values = (1..n).map { value }

            // init table
            transaction(db) {
                SchemaUtils.drop(Structures)
                SchemaUtils.drop(NodeTypeSets)
                SchemaUtils.create(Structures)
                SchemaUtils.create(NodeTypeSets)
            }

            // write
            val writer = StructuresWriter(db)
            println("writing: $values")
            writer.write(values)
            println("wrote: $values")

            // read
            val reader = StructuresReader(db)
            val readValues = reader.read().toList()
            readValues.forEach {
                println("read: $it")
            }
            readValues shouldBe values
        }

        "!CodeProcessor can recode Structures" {
            val value = javaCode
                    .let { processor.parse(it) }

            // init table
            transaction(db) {
                SchemaUtils.drop(Structures)
                SchemaUtils.drop(NodeTypeSets)
                SchemaUtils.create(Structures)
                SchemaUtils.create(NodeTypeSets)
            }

            // write
            println("writing: $value")
            processor.write(value)
            println("wrote: $value")

            // read
            val reader = StructuresReader(db)
            val readValues = reader.read().toList()
            readValues.forEach {
                println("read: $it")
            }

            val orig = value.toSerializable(nodeTypeSet)
            readValues.firstOrNull() shouldBe orig
        }

        "Test faster writer" - {
            "StructuresJsonWriter can record Structure" {
                val value = javaCode
                        .let {
                            val ast = processor.parse(it)
                            processor.proceess(ast)!!
                        }
                        .toSerializable(nodeTypeSet)
                val n = 4
                val values = (1..n).map { value }

                // create file
                val file = createTempFile(directory = path.parent.toFile(), prefix = "ast", suffix = ".structures.jsons")
                file.deleteOnExit()

                // write
                println("writing: $values")
                StructuresJsonWriter(file, nodeTypeSet)
                        .write(values)
                println("wrote.")

                // check
                val text = file.readText()
                println("file:")
                val expected = jacksonObjectMapper().writeValueAsString(nodeTypeSet) + "\n" +
                        values.joinToString("\n") { """{"${encodeByteArray(it.hash)}":${jacksonObjectMapper().writeValueAsString(it.ast)}}""" } + "\n"

                text.asIterable().zip(expected.asIterable()).forEach { (it, other) ->
                    print(it)
                    it shouldBe other
                }
                text shouldBe expected
            }

            "CodeProcessor can write Structures" {
                val value = javaCode
                        .let { processor.parse(it) }
                val n = 4
                val values = (1..n).map { value }

                // write
                println("writing: $values")
                processor.write(values)
                println("wrote.")
            }

            "recorded Structures are readable" {
                val ast = processor.parse(javaCode)
                val value = processor.proceess(ast)!!

                // create file
                val file = createTempFile(directory = path.parent.toFile(), prefix = "ast", suffix = ".structures.jsons")
                        .let {
                            it.delete()
                            it.toPath()
                        }

                // write
                println("writing: $value")
                JsonCodeRecorder(nodeTypeSet, file)
                        .write(value)
                println(value.getText())
                println("wrote.")

                // read
                val text = file.toFile().readLines().drop(1).first()
                println("parsing:")
                val objs = jacksonObjectMapper().readValue<Map<String, SerializableAst.Node>>(text)
                val (hash, node) = objs.entries.first()
                println("hash=$hash")
                println(node.text)
                println("parsed.")

                // check
                node.text shouldBe value.getText()
                decodeByteArray(hash) shouldBe value.toHash()
            }
        }
    }


    private val javaCode = """package sample;
        class SampleClass {
          public static void main() {
            if (cond) invoke();
          }
        }
    """.trimIndent()

    private fun GrammarConfig.getParser(): Parser {
        val (_, parser) = CommonParser(grammarFilePaths, utilJavaFilePaths)
                .parse(input = "package sample;", startRuleName = startRule)
        return parser
    }
}