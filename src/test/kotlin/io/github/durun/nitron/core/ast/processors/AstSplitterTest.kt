package io.github.durun.nitron.core.ast.processors

import io.github.durun.nitron.core.ast.path.AstPath
import io.github.durun.nitron.core.ast.type.nodeTypePoolOf
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.ParserStore
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Paths

class AstSplitterTest : FreeSpec({
    val config = LangConfigLoader.load(Paths.get("config/lang/java.json"))
    val parser = ParserStore.getOrThrow(config.grammar)
    val javaAst = parser.parse(javaCode.reader(), config.grammar.startRule)
        .accept(AstBuildVisitor("java", parser.antlrParser))
    val types = nodeTypePoolOf("java", parser.antlrParser)

    "split statement" {
        val ast = javaAst.copy()
        println(ast)
        config.process.splitConfig.splitRules
        val splitter = AstSplitter(
            config.process.splitConfig.splitRules.map { AstPath.of(it, types) },
            config.process.splitConfig.splitRules.mapNotNull { types.getType(it) }
        )
        val splitted = splitter.process(ast)
        println(splitted)
        splitted.joinToString("\n") shouldBe splittedCode
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
if ( false )
{
x = y * x + 1 ;
}
}
}
<EOF>"""
