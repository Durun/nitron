package io.github.durun.nitron.test

import io.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import java.nio.file.Paths

class JavaNormalizeTest : FreeSpec() {
    private val javaConfig: LangConfig
    private val processor: CodeProcessor

    init {
        val configPath = Paths.get("config/nitron.json")
        javaConfig = NitronConfigLoader.load(configPath).langConfig["java"] ?: throw Exception()
        processor = CodeProcessor(javaConfig)
        "basic" - {
            "package" {
                "package sample;"
                        .toNormStatements() shouldBe emptyList()
            }
            "import" {
                """ package sample;
                import org.sample.GodClass;
            """.trimIndent()
                        .toNormStatements() shouldBe emptyList()
            }
        }
        "class" - {
            "package private class" {
                """ package sample;
                class SampleClass {
                }
                """.trimIndent()
                        .toNormStatements() shouldBe listOf(
                        "class SampleClass {"
                )
            }
            "public class" {
                """ package sample;
                public class SampleClass {
                }
                """.trimIndent()
                        .toNormStatements() shouldBe listOf(
                        "class SampleClass {"
                )
            }
            "class with main()" {
                code().toNormStatements() shouldBe normStatements()
            }
        }
        "space strictness" - {
            val code = code().toNormStatements()
            val statements = normStatements()
            "not strict spaces" {
                val removeSpace = { it: String -> it.replace(" ", "") }
                code.map(removeSpace) shouldBe statements.map(removeSpace)
            }
            "not strict margins" {
                code.map { it.trim() } shouldBe statements.map { it.trim() }
            }
            "strict all characters" {
                code shouldBe statements
            }
        }
        "statement" - {
            "if" - {
                "if statement" {
                    code(methodBody = "if (cond) invoke();")
                            .toNormStatements() shouldBe
                            normStatements(methodBody = "if ( \$V0 ) invoke ( ) ;")
                }
                "if else statement" {
                    code(methodBody = "if (cond) invoke(); else exit();")
                            .toNormStatements() shouldBe
                            normStatements(methodBody = listOf(
                                    "if ( \$V0 ) invoke ( ) ;",
                                    "else exit ( ) ;"
                            ))
                }
            }
        }
    }


    private fun code(
            classBody: String = "",
            methodBody: String = ""
    ): String {
        return """package sample;
            import org.sample.GodClass;
            class SampleClass {
              $classBody
              public static void main() {
              $methodBody
              } 
            }
        """.trimIndent()
    }

    private fun normStatements(
            classBody: List<String> = emptyList(),
            methodBody: List<String> = emptyList()
    ): List<String> {
        return listOf(
                "class SampleClass {",
                *classBody.toTypedArray(),
                "void main ( ) {",
                *methodBody.toTypedArray()
        )
    }

    private fun normStatements(
            classBody: String = "",
            methodBody: String
    ) = normStatements(listOf(classBody), listOf(methodBody))

    private fun String.toStatements(): List<String> {
        return processor.split(this)
                .map { it.getText() }
    }

    private fun String.toNormStatements(): List<String> {
        return processor.split(this)
                .mapNotNull { processor.proceess(it)?.getText() }
    }
}