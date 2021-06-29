package io.github.durun.nitron.core.ast.processors

import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Paths

class AstSplitterTest : FreeSpec({
    val config = LangConfigLoader.load(Paths.get("config/lang/java.json"))
    val parser = config.parserConfig.getParser()
    val javaAst = parser.parse(javaCode.reader())
    val types = parser.nodeTypes

    "split statement" {
        val ast = javaAst.copy()
        println(ast)
        config.processConfig.splitConfig.splitRules
        val splitter = AstSplitter(
            config.processConfig.splitConfig.splitRules.mapNotNull { types.getType(it) }
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
