package com.github.durun.nitron.validation

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.matchers.shouldBe
import java.nio.file.Paths


private val configPath = Paths.get("config/nitron.json")
private val config = NitronConfigLoader.load(configPath).langConfig["cpp-srcml"] ?: throw Exception()

private val processor = CodeProcessor(config)

fun main() = testReportAsMarkDown {
    "class" - {
        "extract class declaration" {
            "class SampleClass {};".normalized() shouldBe listOf("class SampleClass {")
        }
        "extract method declaration" {
            code().normalized() shouldBe normStatements(null)
        }
        "extract member declaration" {
            code(
                classBody = """
                public:
                    int v;
                    void method(int arg);
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                classBody = listOf(
                    "int \$V0 ;",
                    "void method ( int \$V0 ) ;"
                )
            )
        }
    }
    "normalize literals" - {
        "string" {
            code(methodBody = """call("hello");""").normalized() shouldBe normStatements(methodBody = """call ( "S" ) ;""")
        }
        "char" {
            code(methodBody = "call('A');").normalized() shouldBe normStatements(methodBody = "call ( 'C' ) ;")
        }
        "integer" {
            code(methodBody = "call(123);").normalized() shouldBe normStatements(methodBody = "call ( N ) ;")
        }
        "long" {
            code(methodBody = "call(123L);").normalized() shouldBe normStatements(methodBody = "call ( N ) ;")
        }
        "floating point" {
            code(methodBody = "call(3.14);").normalized() shouldBe normStatements(methodBody = "call ( N ) ;")
            code(methodBody = "call(3.14f);").normalized() shouldBe normStatements(methodBody = "call ( N ) ;")
        }
        /*
        "boolean" {
            code(methodBody = "call(true);").normalize() shouldBe normStatements(methodBody = "call ( \$L ) ;")
            code(methodBody = "call(false);").normalize() shouldBe normStatements(methodBody = "call ( \$L ) ;")
        }
         */
    }
    "normalize variables" - {
        "assignment" {
            code(methodBody = "x = f();").normalized() shouldBe normStatements(methodBody = "\$V0 = f () ;")
        }
        "variable declaration" {
            code(methodBody = "int x = f();").normalized() shouldBe normStatements(methodBody = "int \$V0 = f () ;")
            code(methodBody = "int x, y;").normalized() shouldBe normStatements(methodBody = "int \$V0 , \$V1 ;")
        }
        "argument" {
            code(methodBody = "f(a1);").normalized() shouldBe normStatements(methodBody = "f ( \$V0 ) ;")
        }
        "correct variable index" {
            code(methodBody = "f(a0, a1, a0, a2);").normalized() shouldBe normStatements(methodBody = "f ( \$V0 , \$V1 , \$V0 , \$V2 ) ;")
        }
        "expression" {
            code(methodBody = "f((x+y)*z);").normalized() shouldBe normStatements(methodBody = "f ( ( \$V0 + \$V1 ) * \$V2 ) ;")
            code(methodBody = "f(x+f());").normalized() shouldBe normStatements(methodBody = "f ( \$V0 + f () ) ;")
        }
        "member field access" {
            code(methodBody = "f(m.name);").normalized() shouldBe normStatements(methodBody = "f ( \$V0 . name ) ;")
            code(methodBody = "f(m.name.length);").normalized() shouldBe normStatements(methodBody = "f ( \$V0 . name . length ) ;")
        }
        "member method access" {
            code(methodBody = "m.toString();").normalized() shouldBe normStatements(methodBody = "\$V0 . toString () ;")
            code(methodBody = "m.name.toString();").normalized() shouldBe normStatements(methodBody = "\$V0 . name . toString () ;")
        }
    }
    "split statements" - {
        "expression statements" {
            code(
                methodBody = """
                invoke1();
                invoke2();
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                methodBody = listOf(
                    "invoke1 () ;",
                    "invoke2 () ;"
                )
            )
        }
        "try-catch statements" {
            code(
                methodBody = """
                invoke();
                try {
                  invoke1();
                } catch (Exception e) {
                  invoke2();
                }
            """.trimIndent()
            ).normalized() shouldBe normStatements(
                methodBody = listOf(
                    "invoke () ;",
                    "try {",
                    "invoke1 () ;",
                    "catch ( Exception \$V0 ) {",
                    "invoke2 () ;"
                )
            )
        }
        "while statement" {
            code(methodBody = "while(cond) invoke();").normalized() shouldBe normStatements(
                methodBody = listOf(
                    "while ( \$V0 )",
                    "invoke () ;"
                )
            )
        }
        "while block" {
            code(
                methodBody = """
                while(cond) {
                  invoke();
                }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                methodBody = listOf(
                    "while ( \$V0 ) {",
                    "invoke () ;"
                )
            )
        }
        "do-while block" {
            code(
                methodBody = """
                do {
                  invoke();
                }while(cond);
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                methodBody = listOf(
                    "do {",
                    "invoke () ;",
                    "while ( \$V0 ) ;",
                )
            )
        }
        "for statement" {
            code(methodBody = "for(int i=0; i<5; i++) invoke();").normalized() shouldBe normStatements(
                methodBody = listOf(
                    "for ( int \$V0 = N ; \$V0 < N ; \$V0 ++ )",
                    "invoke () ;"
                )
            )
        }
        "for block" {
            code(
                methodBody = """
                for(int i=0; i<5; i++) {
                  invoke();
                  continue;
                }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                methodBody = listOf(
                    "for ( int \$V0 = N ; \$V0 < N ; \$V0 ++ ) {",
                    "invoke () ;",
                    "continue;"
                )
            )
        }
        "if statement" {
            code(methodBody = "if(cond) invoke();").normalized() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 )",
                    "invoke () ;"
                )
            )
        }
        "if block" {
            code(
                methodBody = """
                    if(cond) {
                      invoke1();
                      invoke2();
                    }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 ) {",
                    "invoke1 () ;",
                    "invoke2 () ;"
                )
            )
        }
        "if-else statement" {
            code(methodBody = "if (cond) f1(); else f2();").normalized() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 )",
                    "f1 () ;",
                    "else",
                    "f2 () ;"
                )
            )
        }
        "if-else block" {
            code(methodBody = "if (cond) { f1(); } else { f2(); }").normalized() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 ) {",
                    "f1 () ;",
                    "else {",
                    "f2 () ;"
                )
            )
        }
        "else-if statement" {
            code(methodBody = "if (cond0) f1(); else if (cond1) f2();").normalized() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 )",
                    "f1 () ;",
                    "else if ( \$V0 )",
                    "f2 () ;"
                )
            )
        }
        "switch statement" {
            code(
                methodBody = """
                    switch(a) {
                      case 1:
                      break;
                      default:
                    }
                """.trimIndent()
            ).normalized() shouldBe normStatements(
                methodBody = listOf(
                    "switch ( \$V0 ) {",
                    "case N :",
                    "break;",
                    "default:"
                )
            )
        }
        "return statement" {
            code(methodBody = "return invoke();").normalized() shouldBe normStatements(methodBody = "return invoke () ;")
        }
        "throw statement" {
            code(methodBody = "throw e;").normalized() shouldBe normStatements(methodBody = "throw \$V0 ;")
        }
    }
    "ignoring" - {
        "using declaration" {
            "using namespace std;".normalized() shouldBe emptyList()
        }
        "modifier" {
            code(methodBody = "const int x;").normalized() shouldBe normStatements(methodBody = "int \$V0 ;")
        }
    }
}


private fun code(
    classBody: String = "",
    methodBody: String = ""
): String {
    return """
        class SampleClass {
          $classBody
        };
        int main() {
          invoke();
          $methodBody
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
        "int main () {",
        "invoke () ;",
        *methodBody.toTypedArray()
    )
}

private fun normStatements(
    classBody: String? = null,
    methodBody: String? = null
) = normStatements(listOfNotNull(classBody), listOfNotNull(methodBody))

private fun String.normalized(): List<String> {
    return processor.split(this)
        .mapNotNull { processor.proceess(it) }
        .onEach { println(it.accept(AstPrintVisitor)) }
        .onEach { println(it.accept(AstXmlBuildVisitor)) }
        .map { it.getText() }
}