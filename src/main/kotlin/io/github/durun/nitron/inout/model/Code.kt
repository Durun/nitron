package io.github.durun.nitron.inout.model

import java.security.MessageDigest


class Code(
        val softwareName: String,
        val rawText: String,
        val normalizedText: String,
        val range: IntRange,
        val id: Int? = null
) {
    companion object {
        private val md5: MessageDigest = MessageDigest.getInstance("MD5")
    }

    val hash: ByteArray
        get() = md5.digest(normalizedText.toByteArray())
    val hashString: String
        get() = hash.toString()

    override fun toString(): String {
        return "Code($hash)"
    }

    override fun hashCode(): Int {
        return arrayOf(
                softwareName,
                rawText,
                normalizedText,
                range
        ).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Code

        if (softwareName != other.softwareName) return false
        if (rawText != other.rawText) return false
        if (normalizedText != other.normalizedText) return false
        if (range != other.range) return false

        return true
    }
}