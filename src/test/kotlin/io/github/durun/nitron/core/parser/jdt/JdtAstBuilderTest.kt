package io.github.durun.nitron.core.parser.jdt

import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import io.github.durun.nitron.core.ast.visitor.flatten
import io.github.durun.nitron.core.parser.AstBuilders
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.should
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
          void greet() { 
            System.out.println("Hello, World"); 
          }
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

        ast.flatten().forAll { token ->
            token should { it is AstTerminalNode }
        }

        val tokens = ast.flatten().filterIsInstance<AstTerminalNode>()
        tokens.forEach {
            println("${it.line}: $it")
        }
        val src2 = tokens.groupBy { it.line }
            .entries.sortedBy { (line, _) -> line }
            .joinToString("\n") { (_, tokens) -> tokens.joinToString("") }
        println(src)
        println(src2)
        src2.filter { it != ' ' } shouldBe src.filter { it != ' ' }
    }
})

private fun String.removeSpaceAndNL() = this.filterNot { it in " \n" }
