package com.github.durun.nitron.validation

import com.github.durun.nitron.binding.cpanalyzer.CodeProcessor
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import com.github.durun.nitron.core.ast.visitor.AstXmlBuildVisitor
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.matchers.shouldBe
import java.nio.file.Paths


private val configPath = Paths.get("config/nitron.json")
private val config = NitronConfigLoader.load(configPath).langConfig["php"] ?: throw Exception()

private val processor = CodeProcessor(config)

fun main() = testReportAsMarkDown {
    "namespace" - {
        "ignore namespace" {
            code("namespace sample;").normalized() shouldBe emptyList()
            code("namespace sample { f(); }").normalized() shouldBe listOf("f ( ) ;")
        }
        "ignore use" {
            code("use sample;").normalized() shouldBe emptyList()
            // code("use sample as s;").normalized() shouldBe emptyList()
            code("use function sample\\f;").normalized() shouldBe emptyList()
        }
    }
    "literal" - {
        "string" {
            code(""""hello"""").normalized() shouldBe listOf(""""S"""")
        }
        "numeric" {
            code("123").normalized() shouldBe listOf("N")
            code("0b1").normalized() shouldBe listOf("N")
            code("01").normalized() shouldBe listOf("N")
            code("0x1").normalized() shouldBe listOf("N")
        }
        "floating point" {
            code("1.2").normalized() shouldBe listOf("R")
        }
    }
    "variable" - {
        "assignment" {
            code("\$x = f();").normalized() shouldBe listOf("\$V0 = f ( ) ;")
        }
        "argument" {
            code("f(\$a);").normalized() shouldBe listOf("f ( \$V0 ) ;")
        }
        "correct variable index" {
            code("f(\$a0, \$a1, \$a0, \$a2);").normalized() shouldBe listOf("f ( \$V0 , \$V1 , \$V0 , \$V2 ) ;")
        }
        "expression" {
            code("f((\$x+\$y)*\$z);").normalized() shouldBe listOf("f ( ( \$V0 + \$V1 ) * \$V2 ) ;")
            code("f(\$x+f());").normalized() shouldBe listOf("f ( \$V0 + f ( ) ) ;")
        }
        "class member access" {
            code("\$v->m = f();").normalized() shouldBe listOf("\$V0 -> m = f ( ) ;")
            code("echo \$v->name->length").normalized() shouldBe listOf("echo \$V0 -> name -> length")
        }
    }
    "split statements" - {
        "expression statements" {
            code(
                """
                call1();
                call2();
                """.trimIndent()
            ).normalized() shouldBe listOf("call1 ( ) ;", "call2 ( ) ;")
        }
        "while block" {
            code(
                """
                while (${"$"}i < 5) {
                  call();
                }
                """.trimIndent()
            ).normalized() shouldBe listOf("while ( \$V0 < N )", "{", "call ( ) ;", "}")
            code(
                """
                while (${"$"}i < 5):
                  call();
                endwhile;
                """.trimIndent()
            ).normalized() shouldBe listOf("while ( \$V0 < N ) :", "call ( ) ;", "endwhile ;")
        }
        "do-while block" {
            code(
                """
                do {
                  break;
                } while (0);
                """.trimIndent()
            ).normalized() shouldBe listOf("do", "{", "break ;", "}", "while ( N ) ;")
        }
        "for block" {
            code(
                """
                for (${"$"}i = 0; ${"$"}i < 10; ${"$"}i++) {
                  call();
                }
                """.trimIndent()
            ).normalized() shouldBe listOf("for ( \$V0 = N ; \$V0 < N ; \$V0 ++ )", "{", "call ( ) ;", "}")
            code(
                """
                for (${"$"}i = 0; ${"$"}i < 10; ${"$"}i++):
                  call();
                endfor;
                """.trimIndent()
            ).normalized() shouldBe listOf("for ( \$V0 = N ; \$V0 < N ; \$V0 ++ ) :", "call ( ) ;", "endfor ;")
        }
        "foreach block" {
            code(
                """
                foreach (${"$"}arr as &${"$"}value) {
                  call();
                }
                """.trimIndent()
            ).normalized() shouldBe listOf("foreach ( \$V0 as & \$V1 )", "{", "call ( ) ;", "}")
            code(
                """
                foreach (${"$"}arr as ${"$"}key => ${"$"}value) {
                  call();
                }
                """.trimIndent()
            ).normalized() shouldBe listOf("foreach ( \$V0 as \$V1 => \$V2 )", "{", "call ( ) ;", "}")
        }
        "if block" {
            code(
                """
                if (0) {
                  call();
                }
                """.trimIndent()
            ).normalized() shouldBe listOf("if ( N )", "{", "call ( ) ;", "}")
        }
        "if statement" {
            code(
                """
                if (1) call();
                """.trimIndent()
            ).normalized() shouldBe listOf("if ( N )", "call ( ) ;")
        }
        "if-else block" {
            code(
                """
                if (1) {
                  call();
                } else {
                  call();
                }
            """.trimIndent()
            ).normalized() shouldBe listOf(
                "if ( N )",
                "{",
                "call ( ) ;",
                "}",
                "else",
                "{",
                "call ( ) ;",
                "}"
            )
        }
        "elseif statement" {
            code(
                """
                if (1) call();
                elseif (1) call();
                else call();
                """.trimIndent()
            ).normalized() shouldBe listOf(
                "if ( N )", "call ( ) ;",
                "elseif ( N )", "call ( ) ;",
                "else", "call ( ) ;"
            )
        }
        "switch" {
            code(
                """
                switch ( 1 ) {
                case 1:
                  break;
                default:
                  break;
                }
                """.trimIndent()
            ).normalized() shouldBe listOf(
                "switch ( N ) {",
                "case N :",
                "break ;",
                "default :",
                "break ;",
                "}"
            )
        }
        "return statement" {
            code("return call();").normalized() shouldBe listOf("return call ( ) ;")
        }
        "try-catch-finally block" {
            code(
                """
                try {
                  call();
                } catch (Exception ${"$"}e) {
                  call();
                } finally {
                  call();
                }
                """.trimIndent()
            ).normalized() shouldBe listOf(
                "try {",
                "call ( ) ;",
                "} catch ( Exception \$V0 ) {",
                "call ( ) ;",
                "} finally {",
                "call ( ) ;",
                "}"
            )
        }
    }
    "data" - {
        "array" {
            code(
                """
                [
                  1 => "one",
                  2 => "two",
                ]
                """.trimIndent()
            ).normalized() shouldBe listOf("""[ N => "S" , N => "S" , ]""")
        }
        "class" {
            code(
                """
                class Sample {
                  public ${"$"}property = 'a default value';
                  public function f() {
                    echo ${"$"}this->property;
                  }
                }
                """.trimIndent()
            ).normalized() shouldBe listOf(
                "class Sample {",
                "\$property = \"S\" ;",
                "function f ( ) {",
                "echo \$V0 -> property ;",
                "}",
                "}"
            )
        }
        "closure" {
            code("function (\$a) { return 1; }").normalized() shouldBe listOf(
                "function ( \$V0 ) {",
                "return N ;",
                "}"
            )
        }
    }
    "declaration" - {
        "function" {
            code(
                """
                function f(${"$"}arg) {
                  return 1;
                }
                """.trimIndent()
            ).normalized() shouldBe listOf(
                "function f ( \$V0 ) {",
                "return N ;",
                "}"
            )
        }
    }
}


private fun code(
    body: String = ""
): String {
    return """
        <?php
        $body
        ?>
    """.trimIndent()
}

private fun String.normalized(): List<String> {
    return processor.split(this)
        .mapNotNull { processor.proceess(it) }
        .onEach { println(it.accept(AstPrintVisitor)) }
        .onEach { println(it.accept(AstXmlBuildVisitor)) }
        .map { it.getText() }
}