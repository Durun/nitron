package com.github.durun.nitron.validation

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.matchers.shouldBe
import java.nio.file.Paths


private val configPath = Paths.get("config/nitron.json")
private val config = NitronConfigLoader.load(configPath).langConfig["csharp-srcml"] ?: throw Exception()

private val processor = CodeProcessor(config)

fun main() = testReportAsMarkDown {
    "class" - {
        "extract class declaration" {
            """
                class SampleClass {}
                """.trimIndent().normalize() shouldBe listOf(
                "class SampleClass {}"
            )
        }
        "extract method declaration" {
            code().normalize() shouldBe normStatements(null)
        }
    }
    "normalize literals" - {
        "string" {
            code(methodBody = """call("hello");""").normalize() shouldBe normStatements(methodBody = """call ( "S" ) ;""")
        }
        "char" {
            code(methodBody = "call('A');").normalize() shouldBe normStatements(methodBody = "call ( 'C' ) ;")
        }
        "integer" {
            code(methodBody = "call(123);").normalize() shouldBe normStatements(methodBody = "call ( N ) ;")
        }
        "long" {
            code(methodBody = "call(123L);").normalize() shouldBe normStatements(methodBody = "call ( N ) ;")
        }
        "floating point" {
            code(methodBody = "call(3.14);").normalize() shouldBe normStatements(methodBody = "call ( N ) ;")
            code(methodBody = "call(3.14f);").normalize() shouldBe normStatements(methodBody = "call ( N ) ;")
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
            code(methodBody = "x = f();").normalize() shouldBe normStatements(methodBody = "\$V0 = f () ;")
        }
        "variable declaration" {
            code(methodBody = "int x = f();").normalize() shouldBe normStatements(methodBody = "int \$V0 = f () ;")
            code(methodBody = "int x, y;").normalize() shouldBe normStatements(methodBody = "int \$V0 , \$V1 ;")
        }
        "argument" {
            code(methodBody = "f(a1);").normalize() shouldBe normStatements(methodBody = "f ( \$V0 ) ;")
        }
        "correct variable index" {
            code(methodBody = "f(a0, a1, a0, a2);").normalize() shouldBe normStatements(methodBody = "f ( \$V0 , \$V1 , \$V0 , \$V2 ) ;")
        }
        "expression" {
            code(methodBody = "f((x+y)*z);").normalize() shouldBe normStatements(methodBody = "f ( ( \$V0 + \$V1 ) * \$V2 ) ;")
            code(methodBody = "f(x+f());").normalize() shouldBe normStatements(methodBody = "f ( \$V0 + f () ) ;")
        }
        "member field access" {
            code(methodBody = "f(m.name);").normalize() shouldBe normStatements(methodBody = "f ( \$V0 . name ) ;")
            code(methodBody = "f(m.name.length);").normalize() shouldBe normStatements(methodBody = "f ( \$V0 . name . length ) ;")
        }
        "member method access" {
            code(methodBody = "m.toString();").normalize() shouldBe normStatements(methodBody = "\$V0 . toString () ;")
            code(methodBody = "m.name.toString();").normalize() shouldBe normStatements(methodBody = "\$V0 . name . toString () ;")
        }
    }
    "split statements" - {
        "expression statements" {
            code(
                methodBody = """
                invoke1();
                invoke2();
            """.trimIndent()
            ).normalize() shouldBe normStatements(
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
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "invoke () ;",
                    "try {",
                    "invoke1 () ;",
                    "catch ( Exception \$V0 ) {",
                    "invoke2 () ;"
                )
            )
        }
        "try-catch-finally statements" {
            code(
                methodBody = """
                invoke();
                try {
                  invoke1();
                } catch (Exception e) {
                  invoke2();
                } finally {
                  invoke3();
                }
            """.trimIndent()
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "invoke () ;",
                    "try {",
                    "invoke1 () ;",
                    "catch ( Exception \$V0 ) {",
                    "invoke2 () ;",
                    "finally {",
                    "invoke3 () ;"
                )
            )
        }
        "while statement" {
            code(methodBody = "while(cond) invoke();").normalize() shouldBe normStatements(
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
            ).normalize() shouldBe normStatements(
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
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "do {",
                    "invoke () ;",
                    "while ( \$V0 ) ;",
                )
            )
        }
        "for statement" {
            code(methodBody = "for(int i=0; i<5; i++) invoke();").normalize() shouldBe normStatements(
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
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "for ( int \$V0 = N ; \$V0 < N ; \$V0 ++ ) {",
                    "invoke () ;",
                    "continue;"
                )
            )
        }
        "foreach block" {
            code(
                methodBody = """
                foreach(var i in Items()) {
                  invoke(i);
                  continue;
                }
                """.trimIndent()
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "foreach ( var \$V0 in Items () ) {",
                    "invoke ( \$V0 ) ;",
                    "continue;"
                )
            )
        }
        "if statement" {
            code(methodBody = "if(cond) invoke();").normalize() shouldBe normStatements(
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
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 ) {",
                    "invoke1 () ;",
                    "invoke2 () ;"
                )
            )
        }
        "if-else statement" {
            code(methodBody = "if (cond) f1(); else f2();").normalize() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 )",
                    "f1 () ;",
                    "else",
                    "f2 () ;"
                )
            )
        }
        "if-else block" {
            code(methodBody = "if (cond) { f1(); } else { f2(); }").normalize() shouldBe normStatements(
                methodBody = listOf(
                    "if ( \$V0 ) {",
                    "f1 () ;",
                    "else {",
                    "f2 () ;"
                )
            )
        }
        "else-if statement" {
            code(methodBody = "if (cond0) f1(); else if (cond1) f2();").normalize() shouldBe normStatements(
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
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "switch ( \$V0 ) {",
                    "case N :",
                    "break;",
                    "default:"
                )
            )
        }
        "checked block" {
            code(
                methodBody = """
                    checked {
                      checked(i);
                    }
                """.trimIndent()
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "checked {",
                    "checked ( \$V0 ) ;"
                )
            )
        }
        "unchecked block" {
            code(
                methodBody = """
                    unchecked {
                      unchecked(i);
                    }
                """.trimIndent()
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "unchecked {",
                    "unchecked ( \$V0 ) ;"
                )
            )
        }
        "unsafe block" {
            code(
                methodBody = """
                    unsafe {
                      invoke();
                    }
                """.trimIndent()
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "unsafe {",
                    "invoke () ;"
                )
            )
        }
        "fixed block" {
            code(
                methodBody = """
                    fixed (int* p = stackalloc int[100]) {
                      invoke();
                    }
                """.trimIndent()
            ).normalize() shouldBe normStatements(
                methodBody = listOf(
                    "fixed ( int * \$V0 = stackalloc int [ N ] ) {",
                    "invoke () ;"
                )
            )
        }
        "return statement" {
            code(methodBody = "return invoke();").normalize() shouldBe normStatements(methodBody = "return invoke () ;")
        }
        "yield statement" {
            code(methodBody = "yield break;").normalize() shouldBe normStatements(methodBody = "yield break ;")
        }
        "lock statement" {
            code(methodBody = "lock (sync) invoke();").normalize() shouldBe normStatements(
                methodBody = listOf(
                    "lock ( \$V0 )",
                    "invoke () ;"
                )
            )
        }
        "using statement" {
            code(methodBody = "using (var a = invoke())").normalize() shouldBe normStatements(methodBody = "using ( var \$V0 = invoke () )")
        }
        "throw statement" {
            code(methodBody = "throw e;").normalize() shouldBe normStatements(methodBody = "throw \$V0 ;")
        }
    }
    "ignoring" - {
        "using declaration" {
            "using System".normalize() shouldBe emptyList()
        }
        "modifier" {
            "public class SampleClass {}".normalize() shouldBe listOf("class SampleClass {}")
            code(classBody = "public int x = 0;").normalize() shouldBe normStatements(classBody = "int \$V0 = N ;")
            code(classBody = "private int x = 0;").normalize() shouldBe normStatements(classBody = "int \$V0 = N ;")
            code(classBody = "private readonly int x = 0;").normalize() shouldBe normStatements(classBody = "int \$V0 = N ;")
            code(classBody = "internal protected int x = 0;").normalize() shouldBe normStatements(classBody = "int \$V0 = N ;")
            code(classBody = "private void f(){}").normalize() shouldBe normStatements(classBody = "void f () {")
            code(classBody = "private class SampleClass(){}").normalize() shouldBe normStatements(classBody = "class SampleClass () {")
        }
    }
}


private fun code(
    classBody: String = "",
    methodBody: String = ""
): String {
    return """
        using System;
        class SampleClass {
          $classBody
          public static void Main() {
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
        "void Main () {",
        *methodBody.toTypedArray()
    )
}

private fun normStatements(
    classBody: String? = null,
    methodBody: String? = null
) = normStatements(listOfNotNull(classBody), listOfNotNull(methodBody))

private fun String.normalize(): List<String> {
    return processor.split(this)
        .mapNotNull { processor.proceess(it) }
        .onEach { println(it.accept(AstPrintVisitor)) }
        .onEach { println(it.accept(AstXmlBuildVisitor)) }
        .map { it.getText() }
}