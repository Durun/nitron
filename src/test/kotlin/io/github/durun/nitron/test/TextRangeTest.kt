package io.github.durun.nitron.test

import io.github.durun.nitron.ast.basic.TextRange
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class TextRangeTest: StringSpec({
    "contains" {
        forall(
                row(0,9,10,20, false),
                row(0,20,10,20, true),
                row(10,20,10,20, true),
                row(10,21,10,20, true),
                row(11,20,10,20, false),
                row(20,20,10,20, false),
                row(21,21,10,20, false)
        ) { startA, endA, startB, endB, result ->
            TextRange(startA, endA).contains(TextRange(startB, endB)) shouldBe result
        }
    }
    "equals" {
        TextRange(1, 1) shouldBe TextRange(1, 1)
        TextRange(0, 1) shouldBe TextRange(0, 1)
        TextRange(0, 1) shouldNotBe TextRange(1, 0)
    }
})