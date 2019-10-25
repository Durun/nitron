package io.github.durun.nitron.test

import io.github.durun.nitron.binding.cpanalyzer.StatementProvider
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row

class CPABindingTest : FreeSpec() {
    init {
        "parse by StatementProvider" - {
            "kotlin" {
                forall(
                        row(
                                "fun main(){val str; var n; n = str.size}",
                                listOf(
                                        "val \$V0",
                                        "var \$V1",
                                        "\$V1 = \$V0 . size"
                                )
                        )
                ) { input, nTexts ->
                    val result = StatementProvider.parse(input, "kotlin").map { it.nText }
                    (result) shouldBe nTexts
                }
            }
            "java" {
                forall(
                        row(
                                """public class ExampleClass {
                                        public static void main(String[] args) {
                                            int num = 100, sum = 0;
                                            for(int i = 1; i <= num; ++i) {
                                                sum += i;
                                            }
                                            System.out.println("Sum = " + sum);
                                        }
                                    }""".trimIndent(),
                                listOf(
                                        "" //TODO
                                )
                        )
                ) { input, nTexts ->
                    val result = StatementProvider.parse(input, "java").map { it.nText }
                    (result) shouldBe nTexts
                }
            }
        }
    }
}