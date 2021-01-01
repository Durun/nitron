package io.github.durun.nitron.validation

import io.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import java.nio.file.Paths

class java : FreeSpec() {
    private val javaConfig: LangConfig
    private val processor: CodeProcessor

    init {
        val configPath = Paths.get("config/nitron.json")
        javaConfig = NitronConfigLoader.load(configPath).langConfig["java"] ?: throw Exception()
        processor = CodeProcessor(javaConfig)
        "basic" - {
            "package" {
                "package sample;".toNormStatements() shouldBe emptyList()
            }
            "import" {
                """ package sample;
                import org.sample.GodClass;
            """.trimIndent().toNormStatements() shouldBe emptyList()
            }
        }
        "class" - {
            "package private class" {
                """ package sample;
                class SampleClass {
                }
                """.trimIndent().toNormStatements() shouldBe listOf(
                        "class SampleClass {"
                )
            }
            "public class" {
                """ package sample;
                public class SampleClass {
                }
                """.trimIndent().toNormStatements() shouldBe listOf(
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
        "splitting" - {
            "if split" {
                code(methodBody = "if(cond) invoke();").toNormStatements() shouldBe normStatements(
                        methodBody = "if ( \$V0 ) invoke ( ) ;"
                )
            }
            "if-else split" {
                code(methodBody = "if (cond) f1(); else f2();").toNormStatements() shouldBe normStatements(
                        methodBody = listOf(
                                "if ( \$V0 ) f1 ( ) ;",
                                "else f2 ( ) ;"
                        )
                )
            }
            "else-if split" {
                code(methodBody = "if (cond0) f1(); else if (cond1) { f2(); }").toNormStatements() shouldBe normStatements(
                        methodBody = listOf(
                                "if ( \$V0 ) f1 ( ) ;",
                                "else if ( \$V1 ) {",
                                "f2 ( ) ;"
                        )
                )
            }
            "while split" {
                code(methodBody = "while(cond) invoke();").toNormStatements() shouldBe normStatements(
                        methodBody = "while ( \$V0 ) invoke ( ) ;"
                )
            }
        }
        "statement" - {
            "string and var"  {
                code(methodBody = "log(\"Message: \" + var);").toNormStatements() shouldBe normStatements(
                        methodBody = "log ( \"S\" + \$V0 ) ;"
                )
            }
            "top level" {
                code(methodBody = "System.out.println(msg);").toNormStatements() shouldBe normStatements(
                        methodBody = "System . \$V0 . println ( \$V1 ) ;"
                )
            }
        }
        "literals" - {
            "boolean literal" - {
                "true" {
                    code(methodBody = "c = true;").toNormStatements() shouldBe normStatements(methodBody = "\$V0 = \$L ;")
                }
                "false" {
                    code(methodBody = "c = false;").toNormStatements() shouldBe normStatements(methodBody = "\$V0 = \$L ;")
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
            classBody: String? = null,
            methodBody: String
    ) = normStatements(classBody?.let { listOf(it) }.orEmpty(), listOf(methodBody))

    private fun String.toStatements(): List<String> {
        return processor.split(this)
                .map { it.getText() }
    }

    private fun String.toNormStatements(): List<String> {
        return processor.split(this)
                .mapNotNull { processor.proceess(it)?.getText() }
    }
}