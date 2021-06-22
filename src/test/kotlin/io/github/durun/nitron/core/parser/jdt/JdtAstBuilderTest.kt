package io.github.durun.nitron.core.parser.jdt

import io.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import io.github.durun.nitron.core.parser.AstBuilders
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class JdtAstBuilderTest : FreeSpec({

    val src = """
        package example;
        import java.util.List;
        interface I {
          void greet();
        }
        private class C implements I, I2 {
          @Override
          void greet() { System.out.println("Hello, World"); }
        }
        public class HelloWorld {
           public static void main(String[] args) { 
              y = (x*x) + 1;
           }
        }
    """.trimIndent()

    "parse" {
        val parser = AstBuilders.jdt()
        val ast = parser.parse(src.reader())
        println(ast)
        println(ast.accept(AstPrintVisitor))
        ast.toString().removeSpaceAndNL() shouldBe src.removeSpaceAndNL()
    }
})

private fun String.removeSpaceAndNL() = this.filterNot { it in " \n" }
