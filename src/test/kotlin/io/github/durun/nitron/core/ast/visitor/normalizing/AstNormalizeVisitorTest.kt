package io.github.durun.nitron.core.ast.visitor.normalizing

import io.github.durun.nitron.core.ast.type.nodeTypePoolOf
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.ParserStore
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Paths

class AstNormalizeVisitorTest : FreeSpec({
	val config = LangConfigLoader.load(Paths.get("config/lang/java.json"))
	val parser = ParserStore.getOrThrow(config.grammar)
	val javaAst = parser.parse(javaCode.reader(), config.grammar.startRule)
		.accept(AstBuildVisitor("java", parser.antlrParser))

	"simple path" {
		val ast = javaAst.copy()
		println(ast)
		val normalizer = astNormalizeVisitorOf(
			types = nodeTypePoolOf("java", parser.antlrParser),
			nonNumberedRuleMap = mapOf(listOf("StringLiteral") to "S"),
			numberedRuleMap = mapOf(listOf("expressionName") to "V")
		)
		val normalized = ast.accept(normalizer)
		println(normalized)
		normalized.toString() shouldBe normalizedCode1
	}
	"path ifThenStatement/expression" {
		val ast = javaAst.copy()
		println(ast)
		val normalizer = astNormalizeVisitorOf(
			types = nodeTypePoolOf("java", parser.antlrParser),
			nonNumberedRuleMap = mapOf(listOf("ifThenStatement", "expression") to "COND"),
			numberedRuleMap = mapOf()
		)
		val normalized = ast.accept(normalizer)
		println(normalized)
		normalized.toString() shouldBe normalizedCode2
	}
})


private const val javaCode = """
public class HelloWorld { 
   public static void main(String[] args) { 
      if (false) x = y + 1;
   }
} 
"""
private const val normalizedCode1 =
	"""public class HelloWorld { public static void main ( String [ ] args ) { if ( false ) V0 = V1 + 1 ; } } <EOF>"""
private const val normalizedCode2 =
	"""public class HelloWorld { public static void main ( String [ ] args ) { if ( COND ) x = y + 1 ; } } <EOF>"""