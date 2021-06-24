package io.github.durun.nitron.core.parser.jdt

import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import io.github.durun.nitron.core.ast.visitor.flatten
import io.github.durun.nitron.core.parser.AstBuilders
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.compiler.ITerminalSymbols

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
              int t = a
              // comment
                * x;
              call(
                arg1,
                arg2
              );
           }
        }
    """.trimIndent()

    "parse" {
        val parser = AstBuilders.jdt()
        val ast = parser.parse(src.reader())
        println(ast)
        println(ast.accept(AstPrintVisitor))
        ast.toString().removeComments().removeSpaceAndNL() shouldBe src.removeComments().removeSpaceAndNL()

        ast.flatten().forAll { token ->
            token should { it is AstTerminalNode }
        }

        val tokens = ast.flatten().filterIsInstance<AstTerminalNode>()
        val src2 = tokens.groupBy { it.line }
            .entries.sortedBy { (line, _) -> line }
            .joinToString("\n") { (_, tokens) -> tokens.joinToString("") }
        println(src)
        println(src2)
        src2.removeSpace() shouldBe src.removeComments().removeSpace()
    }
})

private fun String.removeSpaceAndNL() = this.filterNot { it in " \n" }
private fun String.removeSpace() = this.filterNot { it == ' ' }
private fun String.removeComments(): String {
    var result = ""
    val scanner = ToolFactory.createScanner(false, true, true, JavaCore.VERSION_16)
    scanner.source = this.replace(Regex("\r\n|\r|\n"), "\n").toCharArray()
    var tokenType = scanner.nextToken
    while (tokenType != ITerminalSymbols.TokenNameEOF) {
        val start = scanner.currentTokenStartPosition
        val end = scanner.currentTokenEndPosition
        val token = scanner.source.sliceArray(start..end).joinToString("")
        result += token
        tokenType = scanner.nextToken
    }
    return result
}