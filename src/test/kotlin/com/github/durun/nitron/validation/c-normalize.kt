package com.github.durun.nitron.validation

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.matchers.shouldBe
import java.nio.file.Paths


private val configPath = Paths.get("config/nitron.json")
private val config = NitronConfigLoader.load(configPath).langConfig["c-srcml"] ?: throw Exception()

private val processor = CodeProcessor(config)

fun main() = testReportAsMarkDown {
    "normalize literals" - {
        "string" {
            code(funcBody = """call("hello");""").normalized() shouldBe normStatements(funcBody = """call ( "S" ) ;""")
        }
        "char" {
            code(funcBody = "call('A');").normalized() shouldBe normStatements(funcBody = "call ( 'C' ) ;")
        }
        "number" {
            code(funcBody = "call(123);").normalized() shouldBe normStatements(funcBody = "call ( N ) ;")
        }
    }
    "normalize variables" - {
        "assignment" {
            code(funcBody = "x = f();").normalized() shouldBe normStatements(funcBody = "\$V0 = f () ;")
        }
        "variable declaration" {
            code(funcBody = "int x = f();").normalized() shouldBe normStatements(funcBody = "int \$V0 = f () ;")
            code(funcBody = "int x, y;").normalized() shouldBe normStatements(funcBody = "int \$V0 , \$V1 ;")
        }
        "argument" {
            code(funcBody = "f(a1);").normalized() shouldBe normStatements(funcBody = "f ( \$V0 ) ;")
        }
        "correct variable index" {
            code(funcBody = "f(a0, a1, a0, a2);").normalized() shouldBe normStatements(funcBody = "f ( \$V0 , \$V1 , \$V0 , \$V2 ) ;")
        }
        "expression" {
            code(funcBody = "f((x+y)*z);").normalized() shouldBe normStatements(funcBody = "f ( ( \$V0 + \$V1 ) * \$V2 ) ;")
            code(funcBody = "f(x+f());").normalized() shouldBe normStatements(funcBody = "f ( \$V0 + f () ) ;")
        }
        "member access" {
            code(funcBody = "f(m.name);").normalized() shouldBe normStatements(funcBody = "f ( \$V0 . name ) ;")
            code(funcBody = "f(m.name.length);").normalized() shouldBe normStatements(funcBody = "f ( \$V0 . name . length ) ;")
        }
        "member pointer access" {
            code(funcBody = "f(m->name);").normalized() shouldBe normStatements(funcBody = "f ( \$V0 -> name ) ;")
            code(funcBody = "f(m->name.length);").normalized() shouldBe normStatements(funcBody = "f ( \$V0 -> name . length ) ;")
        }
    }
    "split statements" - {
        "expression statements" {
            code(
                funcBody = """
                invoke1();
                invoke2();
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "invoke1 () ;",
                    "invoke2 () ;"
                )
            )
        }
        "while statement" {
            code(funcBody = "while(cond) invoke();").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "while ( \$V0 )",
                    "invoke () ;"
                )
            )
        }
        "while block" {
            code(
                funcBody = """
                while(cond) {
                  invoke();
                }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "while ( \$V0 ) {",
                    "invoke () ;"
                )
            )
        }
        "do-while block" {
            code(
                funcBody = """
                do {
                  invoke();
                }while(cond);
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "do {",
                    "invoke () ;",
                    "while ( \$V0 ) ;",
                )
            )
        }
        "for statement" {
            code(funcBody = "for(int i=0; i<5; i++) invoke();").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "for ( int \$V0 = N ; \$V0 < N ; \$V0 ++ )",
                    "invoke () ;"
                )
            )
        }
        "for block" {
            code(
                funcBody = """
                for(int i=0; i<5; i++) {
                  invoke();
                  continue;
                }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "for ( int \$V0 = N ; \$V0 < N ; \$V0 ++ ) {",
                    "invoke () ;",
                    "continue;"
                )
            )
        }
        "if statement" {
            code(funcBody = "if(cond) invoke();").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "if ( \$V0 )",
                    "invoke () ;"
                )
            )
        }
        "if block" {
            code(
                funcBody = """
                    if(cond) {
                      invoke1();
                      invoke2();
                    }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "if ( \$V0 ) {",
                    "invoke1 () ;",
                    "invoke2 () ;"
                )
            )
        }
        "if-else statement" {
            code(funcBody = "if (cond) f1(); else f2();").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "if ( \$V0 )",
                    "f1 () ;",
                    "else",
                    "f2 () ;"
                )
            )
        }
        "if-else block" {
            code(funcBody = "if (cond) { f1(); } else { f2(); }").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "if ( \$V0 ) {",
                    "f1 () ;",
                    "else {",
                    "f2 () ;"
                )
            )
        }
        "else-if statement" {
            code(funcBody = "if (cond0) f1(); else if (cond1) f2();").normalized() shouldBe normStatements(
                funcBody = listOf(
                    "if ( \$V0 )",
                    "f1 () ;",
                    "else if ( \$V0 )",
                    "f2 () ;"
                )
            )
        }
        "switch statement" {
            code(
                funcBody = """
                    switch(a) {
                      case 1:
                      break;
                      default:
                    }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                funcBody = listOf(
                    "switch ( \$V0 ) {",
                    "case N :",
                    "break;",
                    "default:"
                )
            )
        }
        "return statement" {
            code(funcBody = "return invoke();").normalized() shouldBe normStatements(funcBody = "return invoke () ;")
        }
    }
    "ignoring" - {
        "include" {
            "#include <stdio.h>".normalized() shouldBe emptyList()
        }
        "modifier" {
            code(global = "const int x = 0;").normalized() shouldBe normStatements(global = "int \$V0 = N ;")
        }
    }
}


private fun code(
    global: String = "",
    funcBody: String = ""
): String {
    return """
        $global
        int main() {
          $funcBody
        }
    """.trimIndent()
}

private fun normStatements(
    global: List<String> = emptyList(),
    funcBody: List<String> = emptyList()
): List<String> {
    return listOf(
        *global.toTypedArray(),
        "int main () {",
        *funcBody.toTypedArray()
    )
}

private fun normStatements(
    global: String? = null,
    funcBody: String? = null
) = normStatements(listOfNotNull(global), listOfNotNull(funcBody))

private fun String.normalized(): List<String> {
    return processor.split(this)
        .mapNotNull { processor.proceess(it) }
        .onEach { println(it.accept(AstPrintVisitor)) }
        .onEach { println(it.accept(AstXmlBuildVisitor)) }
        .map { it.getText() }
}