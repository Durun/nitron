package com.github.durun.nitron.app.preparse

import com.github.durun.nitron.core.toBlob
import com.github.durun.nitron.core.toByteArray
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
                println("Input length : ${input.length}")
                println("Deflated size: ${bytes.size}")
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

        "gzip" {
            Arb.string().take(10).forEach { input ->
                println(input)

                val bytes = input.gzip()
                val output = bytes.ungzip()

                println(output)
                println("Input length : ${input.length}")
                println("Deflated size: ${bytes.size}")
                output shouldBe input
            }
        }
        "gzip over blob" {
            Arb.string().take(10).forEach { input ->
                println(input)

                val blob = input.gzip().toBlob()
                val output = blob.toByteArray().ungzip()

                println(output)
                output shouldBe input
            }
        }
    }
})
