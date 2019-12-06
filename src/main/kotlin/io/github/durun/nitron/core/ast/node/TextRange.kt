package io.github.durun.nitron.core.ast.node

import kotlin.math.max
import kotlin.math.min

/**
 * 文字列の範囲
 */
class TextRange internal constructor(
        /**
         * 行単位
         */
        val line: IntRange
) {
    /**
     * 両者を含む最小の範囲を返す
     */
    fun include(other: TextRange): TextRange {
        return TextRange(
                line = this.line.include(other.line)
        )
    }

    private fun IntRange.include(other: IntRange): IntRange {
        return IntRange(
                start = min(this.first, other.first),
                endInclusive = max(this.last, other.last)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextRange

        if (line != other.line) return false

        return true
    }

    override fun hashCode(): Int {
        return line.hashCode()
    }
}

fun lineRangeOf(start: Int, stop: Int): TextRange {
    return TextRange(
            line = IntRange(start, stop)
    )
}