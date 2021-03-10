package io.github.durun.nitron.core

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string

class MD5Test : FreeSpec({

    "toString" {
        val testData = Arb.string().edgecases() + listOf("hogehoge")
        testData.forEach {
            val md5 = MD5.digest("hogehoge")
            println(md5.toString())
            println(md5.toStringOld())
            md5.toString() shouldBe md5.toStringOld()
        }
    }
})

private fun MD5.toStringOld(): String = String.format(
    "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
    bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
    bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]
)