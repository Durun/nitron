package com.github.durun.nitron.core.ast.processors

import com.github.durun.nitron.core.config.loader.LangConfigLoader
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
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
