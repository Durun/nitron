package io.github.durun.nitron.app.preparse

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.take

class ParseUtilTest : FreeSpec({

    "inflateAndDeflateText" {
        Arb.string().take(10).forEach { input->
            println(input)

            val bytes = input.toByteArray().deflate()
            val output = bytes.inflate().decodeToString()

            println(output)
            output shouldBe input
        }
    }
})
