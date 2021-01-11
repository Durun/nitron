package io.github.durun.nitron.inout.model

import io.github.durun.nitron.core.MD5


class Code(
        val softwareName: String,
        val rawText: String,
        val normalizedText: String,
        val range: IntRange,
        var id: Int? = null
) {
    companion object

    val hash: MD5
        get() = MD5.digest(normalizedText)

    override fun toString(): String {
        return "Code($hash)"
    }

    override fun hashCode(): Int {
        var result = softwareName.hashCode()
        result = 31 * result + rawText.hashCode()
        result = 31 * result + normalizedText.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + (id ?: 0)
        return result
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