package com.github.durun.nitron.core.ast.processors

import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.path.AstPath
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import com.github.durun.nitron.core.config.loader.LangConfigLoader
import com.github.durun.nitron.testutil.astNode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.nio.file.Paths

class AstNormalizerTest : FreeSpec({
	val config = LangConfigLoader.load(Paths.get("config/lang/java.json"))
    val parser = config.parserConfig.getParser()
    val javaAst = parser.parse(javaCode.reader())
    val types = parser.nodeTypes

    "simple path" {
        val ast = javaAst.copy()
        println(ast)
        val normalizer = AstNormalizer(
            mapOf(AstPath.of("IntegerLiteral", types) to "N"),
            numberedMapping = mapOf(AstPath.of("//expressionName", types) to "V"),
            ignoreRules = listOf(AstPath.of("EOF", types))
        )
        val normalized = normalizer.process(ast)
        println(normalized)
        normalized.toString() shouldBe normalizedCode1
    }
    "path ifThenStatement/expression" {
        val ast = javaAst.copy()
        println(ast)
        val normalizer = AstNormalizer(
            mapOf(AstPath.of("//ifThenStatement/expression") to "COND"),
            numberedMapping = emptyMap(),
            ignoreRules = listOf()
        )
        val normalized = normalizer.process(ast)
        println(normalized)
        normalized.toString() shouldBe normalizedCode2
    }

    "traceable check (small tree)" {
        val token = TokenType(0, "TOKEN")
        val semicolon = TokenType(1, "SEMICOLON")
        val rule = RuleType(2, "rule")
        val types = NodeTypePool.of("grammar", tokenTypes = listOf(token, semicolon), ruleTypes = listOf(rule))
        val node = astNode(rule) {
            token("token1", token)
            node(rule) {
                token("token2", token)
                token(";", semicolon)
            }
        }

        val normalizer = AstNormalizer(emptyMap(), emptyMap(), listOf(AstPath.of("SEMICOLON", types)))
        val copied = normalizer.process(node) as AstRuleNode

        listOf(
            copied.originalNode to node,
            copied.children!!.first().originalNode to node.children.first(),
            copied.children!!.last().originalNode to node.children.last(),
            copied.children!!.last().children!!.first().originalNode to node.children.last().children!!.first()
        ).forAll { (ref, original) ->
            ref shouldBe original
            ref shouldBeSameInstanceAs original
        }
    }

    "test" {
        val jdtConfig = LangConfigLoader.load(Paths.get("config/lang/java-jdt.json"))
        val jdtParser = jdtConfig.parserConfig.getParser()
        val splitter = jdtConfig.processConfig.splitConfig.initSplitter(jdtParser.nodeTypes)
        val normalizer = jdtConfig.processConfig.normalizeConfig.initNormalizer(jdtParser.nodeTypes)

        val ast = jdtParser.parse(javaCode2.reader())
        val statements = splitter.process(ast)
        val normalized = statements.map {
            println(it)
            normalizer.process(it)
        }
    }
})

private const val javaCode = """
public class HelloWorld { 
   public static void main(String[] args) { 
      if (false) x = y * x + 1;
   }
} 
"""
private const val normalizedCode1 =
	"""public class HelloWorld { public static void main ( String [ ] args ) { if ( false ) V0 = V1 * V0 + N ; } }"""
private const val normalizedCode2 =
	"""public class HelloWorld { public static void main ( String [ ] args ) { if ( COND ) x = y * x + 1 ; } } <EOF>"""

private const val javaCode2 = """
@RunWith(SampleRunner.class)
public class Sample {
}
"""