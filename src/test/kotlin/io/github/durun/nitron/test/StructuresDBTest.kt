package io.github.durun.nitron.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import io.github.durun.nitron.binding.cpanalyzer.JsonCodeRecorder
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.createNodeTypePool
import io.github.durun.nitron.core.ast.type.nodeTypePoolOf
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
import io.github.durun.nitron.inout.model.ast.table.StructuresJsonWriter
import io.github.durun.nitron.inout.model.ast.toSerializable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.antlr.v4.runtime.Parser
import org.jetbrains.exposed.sql.Database
import java.nio.file.Paths

class StructuresDBTest : FreeSpec() {
    private val path = Paths.get("testdata/database/test.db")
    private val langPath = Paths.get("config/lang/java.json")
    private val langConfig: LangConfig
    private val processor: CodeProcessor
    private val nodeTypePool: NodeTypePool
    private val nodeTypeSet: NodeTypeSet
    private val db: Database

    init {
        db = SQLiteDatabase.connect(path)
        langConfig = LangConfigLoader.load(langPath)
        processor = CodeProcessor(langConfig, outputPath = path.parent.resolve("test.structures"))
        val antlrParser = langConfig.grammar.getParser()    // TODO
        nodeTypePool = nodeTypePoolOf(langConfig.fileName, antlrParser)
        nodeTypeSet = nodeTypePool.toSerializable()

        "prepare" - {
            "Structure is serializable" {
                val value = javaCode
                        .let { processor.parse(it) }
                        .toSerializable(nodeTypePool)
                value.shouldNotBeNull()
            }
        }

        "Test faster writer" - {
            "StructuresJsonWriter can record Structure" {
                val value = javaCode
                        .let {
                            val ast = processor.parse(it)
                            processor.proceess(ast)!!
                        }
                        .toSerializable(nodeTypePool)
                val n = 4
                val values = (1..n).map { value }

                // create file
                val file = createTempFile(directory = path.parent.toFile(), prefix = "ast", suffix = ".structures.jsons")
                file.deleteOnExit()

                // write
                println("writing: $values")
                StructuresJsonWriter(file, nodeTypePool)
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
                JsonCodeRecorder(nodeTypePool, file)
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
        return CommonParser(grammarFilePaths, utilJavaFilePaths).getAntlrParser()
    }
}