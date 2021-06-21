package io.github.durun.nitron.core.parser.jdt

import io.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import io.github.durun.nitron.core.parser.AstBuilders
import io.kotest.core.spec.style.FreeSpec

class JdtAstBuilderTest : FreeSpec({

    val src = """
        package example;
        import java.util.*;
        public class HelloWorld { 
           public static void main(String[] args) { 
              System.out.println("Hello, World");
              y = (x*x) + 1;
           }
        }
    """.trimIndent()

    "parse" {
        val parser = AstBuilders.jdt()
        val ast = parser.parse(src.reader())
        println(ast)
        println(ast.accept(AstPrintVisitor))
    }
})
