package com.github.durun.nitron.core.ast.processors

import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.config.loader.LangConfigLoader
import com.github.durun.nitron.testutil.astNode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.nio.file.Paths

class AstSplitterTest : FreeSpec({
    val config = LangConfigLoader.load(Paths.get("config/lang/java-jdt.json"))
    val parser = config.parserConfig.getParser()
    val types = parser.nodeTypes
    val splitter = config.processConfig.splitConfig.initSplitter(types)

    "split statement" {
        val ast = parser.parse(javaCode.reader())
        println(ast)
        val splitted = splitter.process(ast)
        println(splitted)
        splitted.joinToString("\n") shouldBe splittedCode
    }

    "traceable check (small tree)" {
        val token = TokenType(0, "TOKEN")
        val rule = RuleType(1, "rule")
        val node = astNode(rule) {
            token("token1", token)
            node(rule) {
                token("token2", token)
                token(";", token)
            }
        }

        val splitter = AstSplitter(emptyList())
        val copied = splitter.process(node).first()

        listOf(
            copied.originalNode to node,
            copied.children!!.first().originalNode to node.children!!.first(),
            copied.children!!.last().originalNode to node.children!!.last(),
            copied.children!!.last().children!!.first().originalNode to node.children!!.last().children!!.first()
        ).forAll { (ref, original) ->
            ref shouldBe original
            ref shouldBeSameInstanceAs original
        }
    }

    "statements are traceable" {
        val unit = parser.parse(javaCode.reader())
        println(unit.accept(AstPrintVisitor))

        val statements = splitter.process(unit)
        statements.forEachIndexed { i, it ->
            println("-- $i --")
            println(it.accept(AstPrintVisitor))
        }

        val typeDecl = unit
            .children!!.first { it.type.name == "TYPE_DECLARATION" }
        val methodDecl = typeDecl
            .children!!.first { it.type.name == "METHOD_DECLARATION" }
        val ifStatement = methodDecl
            .children!!.first { it.type.name == "BLOCK" }
            .children!!.first { it.type.name == "IF_STATEMENT" }
        val exprStatement = ifStatement
            .children!!.first { it.type.name == "BLOCK" }
            .children!!.first { it.type.name == "EXPRESSION_STATEMENT" }

        listOf(
            statements[0].originalNode to typeDecl,
            statements[1].originalNode to methodDecl,
            statements[2].originalNode to ifStatement,
            statements[3].originalNode to exprStatement,
            statements[4].originalNode to ifStatement,
            statements[5].originalNode to methodDecl,
            statements[6].originalNode to typeDecl
        ).forAll { (ref, original) ->
            ref shouldBe original
            ref shouldBeSameInstanceAs original
        }
    }
})

private const val javaCode = """
public class HelloWorld { 
  public static void main(String[] args) { 
    if (false) {
      x = y * x + 1;
    }
  }
} 
"""

private const val splittedCode = """public class HelloWorld {
public static void main ( String [ ] args ) {
if ( false ) {
x = y * x + 1 ;
}
}
}"""
