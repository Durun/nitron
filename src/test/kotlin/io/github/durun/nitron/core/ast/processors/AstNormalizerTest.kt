package io.github.durun.nitron.core.ast.processors

import io.github.durun.nitron.core.ast.path.AstPath
import io.github.durun.nitron.core.parser.antlr.nodeTypePoolOf
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.github.durun.nitron.core.parser.ParserStore
import io.github.durun.nitron.core.parser.antlr.AstBuildVisitor
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Paths

class AstNormalizerTest : FreeSpec({
	val config = LangConfigLoader.load(Paths.get("config/lang/java.json"))
	val parser = ParserStore.getOrThrow(config.grammar)
	val javaAst = parser.parse(javaCode.reader(), config.grammar.startRule)
		.accept(AstBuildVisitor("java", parser.antlrParser))
	val types = nodeTypePoolOf("java", parser.antlrParser)

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
