package io.github.durun.nitron.binding.cpanalyzer

import java.security.MessageDigest


class Statement(
        @JvmField
        val tokens: List<Token>,
        @JvmField
        val rText: String,
        @JvmField
        val nText: String
) {
    @JvmField
    val fromLine: Int = tokens.first().line
    @JvmField
    val toLine: Int = tokens.last().line
    @JvmField
    val hash: ByteArray = digester.digest(nText.toByteArray())

    companion object {
        private val digester = MessageDigest.getInstance("MD5")
    }
}

data class Token(
        @JvmField
        val value: String,
        @JvmField
        val line: Int,
        @JvmField
        val index: Int
)