package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.toBlob
import io.github.durun.nitron.core.toByteArray
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.take

class ParseUtilTest : FreeSpec({

    "inflateAndDeflateText" - {
        "text" {
            Arb.string().take(10).forEach { input ->
                println(input)

                val bytes = input.toByteArray().deflate()
                val output = bytes.inflate().decodeToString()

                println(output)
                output shouldBe input
            }
        }
        "over blob" {
            Arb.string().take(10).forEach { input ->
                println(input)

                val bytes = input.toByteArray().deflate().toBlob()
                val output = bytes.toByteArray().inflate().decodeToString()

                println(output)
                output shouldBe input
            }
        }
    }
})
