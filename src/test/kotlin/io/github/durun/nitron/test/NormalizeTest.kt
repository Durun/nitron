package io.github.durun.nitron.test

import io.github.durun.nitron.core.ast.visitor.normalizing.NormalizePrintVisitor
import io.github.durun.nitron.core.ast.visitor.normalizing.NormalizingRuleMap
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.CommonParser
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors


val grammarDir = Paths.get("testdata/grammars/kotlin-formal")
val gramarList = Files.list(grammarDir)
        .filter { it.toString().endsWith(".g4") }
        .collect(Collectors.toList())
val parser = CommonParser(gramarList)
private fun normalize(input: String): String {
    val startRule = "kotlinFile"
    val (tree, antlrParser) = parser.parse(input, startRule)
    val ast = tree.accept(AstBuildVisitor(antlrParser))
    // normalize
    return ast.accept(NormalizePrintVisitor(
            nonNumberedRuleMap = NormalizingRuleMap(
                    listOf("stringLiteral") to "\"S\"",
                    listOf("CharacterLiteral") to "'C'",
                    listOf("IntegerLiteral") to "N",
                    listOf("RealLiteral") to "N"
            ),
            numberedRuleMap = NormalizingRuleMap(
                    listOf("primaryExpression", "simpleIdentifier") to "\$V",
                    listOf("variableDeclaration", "simpleIdentifier") to "\$V",
                    listOf("directlyAssignableExpression", "simpleIdentifier") to "\$V"
            )
    )).replace("(<EOF> *)+".toRegex(), "")
}

class NormalizeTest : StringSpec({
    "kotlin stringLiteral" {
        forall(
                row(
                        "fun main(){\"line string\"}",
                        "fun main(){\"S\"}"
                ),
                row(
                        "fun main(){\"\"\"multi line string\"\"\"}",
                        "fun main(){\"S\"}"
                )
        ) { input, nText ->
            normalize(input).replace(" ", "") shouldBe nText.replace(" ", "")
        }
    }
    "kotlin variableDeclaration" {
        forall(
                row(
                        "fun main(){val n}",
                        "fun main(){val \$V0}"
                ),
                row(
                        "fun main(){val n; val m}",
                        "fun main(){val \$V0; val \$V1}"
                ),
                row(
                        "fun main(){val n; var m}",
                        "fun main(){val \$V0; var \$V1}"
                )
        ) { input, nText ->
            normalize(input).replace(" ", "") shouldBe nText.replace(" ", "")
        }
    }
    "kotlin numbers" {
        forall(
                row(
                        "fun main(){1}",
                        "fun main(){N}"
                ),
                row(
                        "fun main(){3.14}",
                        "fun main(){N}"
                ),
                row(
                        "fun main(){2.0f}",
                        "fun main(){N}"
                )
        ) { input, nText ->
            normalize(input).replace(" ", "") shouldBe nText.replace(" ", "")
        }
    }
    "kotlin variables" {
        forall(
                row(
                        "fun main(){val n; val m = n}",
                        "fun main(){val \$V0; val \$V1 = \$V0}"
                ),
                row(
                        "fun main(){val str; val n = str.size}",
                        "fun main(){val \$V0; val \$V1 = \$V0.size}"
                ),
                row(
                        "fun main(){val str; var n; n = str.size}",
                        "fun main(){val \$V0; var \$V1; \$V1 = \$V0.size}"
                )
        ) { input, nText ->
            normalize(input).replace(" ", "") shouldBe nText.replace(" ", "")
        }
    }
    "kotlin non normalized rules" {
        forall(
                row("fun main(){null}"),
                row("fun main(){true; false}")
        ) { input ->
            normalize(input).replace(" ", "") shouldBe input.replace(" ", "")
        }
    }
})