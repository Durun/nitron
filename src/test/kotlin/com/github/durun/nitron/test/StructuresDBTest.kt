package com.github.durun.nitron.test

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.binding.cpanalyzer.JsonCodeRecorder
import com.github.durun.nitron.core.AstSerializers
import com.github.durun.nitron.core.MD5
import com.github.durun.nitron.core.ast.node.digest
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.config.LangConfig
import com.github.durun.nitron.core.config.loader.LangConfigLoader
import com.github.durun.nitron.core.parser.antlr.ParserStore
import com.github.durun.nitron.core.parser.antlr.nodeTypePoolOf
import com.github.durun.nitron.inout.database.SQLiteDatabase
import com.github.durun.nitron.inout.model.ast.Structure
import com.github.durun.nitron.inout.model.ast.table.StructuresJsonWriter
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.Database
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
class StructuresDBTest : FreeSpec() {
	private val path = Paths.get("testdata/database/test.db")
	private val langPath = Paths.get("config/lang/java.json")
	private val langConfig: LangConfig
	private val processor: CodeProcessor
	private val nodeTypePool: NodeTypePool
	private val db: Database

	init {
		db = SQLiteDatabase.connect(path)
		langConfig = LangConfigLoader.load(langPath)
		processor = CodeProcessor(langConfig, outputPath = path.parent.resolve("test.structures"))
		val antlrParser = ParserStore.getOrThrow(langConfig.parserConfig).antlrParser
		nodeTypePool = nodeTypePoolOf(langConfig.fileName, antlrParser)

		"prepare" - {
			"Structure is serializable" {
				val value = javaCode
						.let { processor.parse(it) }
						.let { Structure(nodeTypePool, it) }
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
                    .let { Structure(nodeTypePool, it) }
                val n = 4
                val values = (1..n).map { value }

                // create file
                val file =
                    kotlin.io.path.createTempFile(directory = path.parent, prefix = "ast", suffix = ".structures.jsons")
                        .toFile()
                file.deleteOnExit()

                // write
                println("writing: $values")
                StructuresJsonWriter(file, nodeTypePool)
                    .write(values)
                println("wrote.")

                // check
                val text = file.readText()
				println("file:")
				val expected = Json.encodeToString(nodeTypePool) + "\n" +
						values.joinToString("\n") { AstSerializers.encodeOnlyJson.encodeToString(it) } + "\n"

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
                val file =
                    kotlin.io.path.createTempFile(directory = path.parent, prefix = "ast", suffix = ".structures.jsons")
                        .also { Files.delete(it) }

                // write
                println("writing: $value")
                JsonCodeRecorder(nodeTypePool, file)
                    .write(value)
                println(value.getText())
                println("wrote.")

                // read
                val text = file.toFile().readLines().drop(1).first()
				println("parsing:")
				val obj: Structure = AstSerializers.json(nodeTypePool).decodeFromString(text)
				val hash = obj.hash
				val node = obj.asts
				println("hash=$hash")
				println(node.joinToString { it.getText() })
				println("parsed.")

				// check
				node.joinToString { it.getText() } shouldBe value.getText()
				hash shouldBe MD5.digest(value)
				Json.parseToJsonElement(text).jsonObject["hash"]?.jsonPrimitive?.content shouldHaveLength 32
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
}