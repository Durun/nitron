package io.github.durun.nitron.binding.cpanalyzer

import java.security.MessageDigest


class Statement(
        val tokens: List<Token>,
        val rText: String,
        val nText: String
) {
    val fromLine: Int = tokens.first().line
    val toLine: Int = tokens.last().line
    val hash: ByteArray = digester.digest(nText.toByteArray())

    companion object {
        private val digester = MessageDigest.getInstance("MD5")
    }
}

data class Token(
        val value: String,
        val line: Int,
        val index: Int
)