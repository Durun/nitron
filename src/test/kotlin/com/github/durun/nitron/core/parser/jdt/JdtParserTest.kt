package com.github.durun.nitron.core.parser.jdt

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.visitor.AstLineGetVisitor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstVisitor
import com.github.durun.nitron.core.ast.visitor.flatten
import com.github.durun.nitron.core.parser.NitronParsers
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.compiler.ITerminalSymbols
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import java.nio.file.Path
import kotlin.io.path.readText

class JdtParserTest : FreeSpec({
    "parse" {
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
        val parser = NitronParsers.jdt()
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

        // line number
        val (beginLine, endLine) = ast.accept(AstLineGetVisitor)
        beginLine shouldBe 1
        endLine shouldBe 23
    }

    "convert test" {
        val src = Path.of("config/grammars/java/java/examples/AllInOne8.java").readText().removeComments()

        val unit = jdtParse(src)
        val converter = AstConvertVisitor()
        unit.accept(converter)
        val ast = converter.result

        println("--nitron Tree--")
        println(ast.accept(LinePrintVisitor))

        // check
        val astLines = run {
            val collector = LineCollectVisitor()
            ast.accept(collector)
            collector.result
        }

        /*
        astLines.zip(src.lines()).forAll { (astLine, textLine) ->
            astLine.joinToString("").removeSpaceAndNL() shouldBe textLine.removeSpaceAndNL()
        }
         */
        src.lines().dropLast(1).withIndex().toList().forAll {
            val astLine = astLines[it.index]
            astLine.joinToString("").removeSpaceAndNL() shouldBe it.value.removeSpaceAndNL()
        }
    }
})

private fun String.removeSpaceAndNL() = this.filterNot { it in " \n" }
private fun String.removeSpace() = this.filterNot { it == ' ' }
private fun String.removeNL() = this.filterNot { it == '\n' }
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

private object LinePrintVisitor : AstVisitor<String> {
    override fun visit(node: AstNode): String = TODO()

    override fun visitRule(node: AstRuleNode): String {
        val thisTokens = node.getText().split(" ").let {
            val n = 3
            it
                .take(n)
                .joinToString(" ") +
                    if (n < it.size) " ..." else ""
        }
        val thisText = "${node.type.name}\t\t$thisTokens"
        val childrenText = node.children.orEmpty()
            .joinToString("\n") { it.accept(this) }
            .prependIndent("\t")
        return thisText + "\n" + childrenText
    }

    override fun visitTerminal(node: AstTerminalNode): String {
        return "${node.line}: ${node.type.name} ${node.getText()}"
    }
}


private class LineCollectVisitor : AstVisitor<Unit> {
    private val lines: MutableMap<Int, MutableList<AstTerminalNode>> = mutableMapOf()
    val result: List<List<AstTerminalNode>>
        get() {
            val maxLineNo = lines.keys.maxOrNull() ?: 1
            return (1..maxLineNo).map { lineNo ->
                lines[lineNo] ?: emptyList()
            }
        }

    private fun add(node: AstTerminalNode, lineNo: Int) {
        val line = lines.computeIfAbsent(lineNo) { mutableListOf() }
        line.add(node)
    }

    override fun visit(node: AstNode): Unit = TODO()
    override fun visitRule(node: AstRuleNode) {
        node.children?.forEach {
            it.accept(this)
        }
    }

    override fun visitTerminal(node: AstTerminalNode) {
        add(node, node.line)
    }
}

private val jdtParser =
    ASTParser.newParser(AST.JLS17)
        .apply {
            val version = JavaCore.VERSION_16

            @Suppress("UNCHECKED_CAST")
            val defaultOptions = DefaultCodeFormatterConstants.getEclipseDefaultSettings() as Map<String, String>
            val options = defaultOptions + mapOf(
                JavaCore.COMPILER_COMPLIANCE to version,
                JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM to version,
                JavaCore.COMPILER_SOURCE to version,
                JavaCore.COMPILER_DOC_COMMENT_SUPPORT to JavaCore.DISABLED
            )
            setCompilerOptions(options)
            setEnvironment(null, null, null, true)
        }

private fun jdtParse(src: String): ASTNode {
    val source = src.replace(Regex("\r\n|\r|\n"), "\n")
    jdtParser.setSource(source.toCharArray())
    return jdtParser.createAST(null)
}
