package io.github.durun.nitron.test

import io.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import java.nio.file.Paths


class CodeProcessorTest : FreeSpec({
    val processor = CodeProcessor(
            config = LangConfigLoader.load(Paths.get("config/lang/java.json"))
    )
    "sample.java" {
        println("Parse: ")
        val ast = processor.parse(javaCode)
        val parsed = ast.getText()
        println(parsed)
        parsed shouldBe javaParsed

        println("\nSplit: ")
        val astList = processor.split(ast)
        val splitted = astList.joinToString("\n") { it.getText() }
        println(splitted)
        splitted shouldBe javaSplitted

        println("\nNormalize: ")
        val normAstList = astList.mapNotNull { processor.proceess(it) }
        val normalized = normAstList.joinToString("\n") { it.getText() }
        println(normalized)
        normalized shouldBe javaNormalized
    }
})


private const val javaCode = """
package sample;
import some.Library;

public class HelloWorld { 
   public static void main(String[] args) { 
        try {
            URL url = null ;
            try {
                url = new URL ( source ) ;
                url = url + "test";
            } catch ( MalformedURLException e ) {
                throw new BuildException ( e . toString ( ) ) ;
            }
        } catch (Exception e) {
            throw e;
        }
        if (true) {
            System.out.println("Hello-A");
        } else if (false) {
            System.out.println("Hello-B");
        }
   }
} 
"""
private const val javaParsed = """package sample ; import some . Library ; public class HelloWorld { public static void main ( String [ ] args ) { try { URL url = null ; try { url = new URL ( source ) ; url = url + "test" ; } catch ( MalformedURLException e ) { throw new BuildException ( e . toString ( ) ) ; } } catch ( Exception e ) { throw e ; } if ( true ) { System . out . println ( "Hello-A" ) ; } else if ( false ) { System . out . println ( "Hello-B" ) ; } } } <EOF>"""
private const val javaSplitted = """package sample ; import some . Library ;
public class HelloWorld {
public static void main ( String [ ] args ) {
try {
URL url = null ;
try {
url = new URL ( source ) ;
url = url + "test" ;
}
catch ( MalformedURLException e ) {
throw new BuildException ( e . toString ( ) ) ;
}
}
catch ( Exception e ) {
throw e ;
}
if ( true ) {
System . out . println ( "Hello-A" ) ;
} else
if ( false )
{
System . out . println ( "Hello-B" ) ;
}
}
}
<EOF>"""
private const val javaNormalized = """class HelloWorld {
void main ( String [ ] ${"$"}V0 ) {
try {
URL ${"$"}V0 = null ;
try {
${"$"}V0 = new URL ( ${"$"}V1 ) ;
${"$"}V0 = ${"$"}V0 + "S" ;
}
catch ( MalformedURLException ${"$"}V0 ) {
throw new BuildException ( ${"$"}V0 . toString ( ) ) ;
}
}
catch ( Exception ${"$"}V0 ) {
throw ${"$"}V0 ;
}
if ( true ) {
${"$"}V0 . println ( "S" ) ;
} else
if ( false )
{
${"$"}V0 . println ( "S" ) ;
}
}
}"""
