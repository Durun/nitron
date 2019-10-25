package io.github.durun.nitron.core.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.math.max
import kotlin.math.min

/**
 * 整数の範囲
 */
class IntRange(
        /**
         * 開始位置
         */
        @JsonProperty("start")
        val start: Int,
        /**
         * 終了位置
         */
        @JsonProperty("stop")
        val stop: Int
) {
    /**
     * 両者を含む最小の範囲を返す
     */
    fun include(other: IntRange): IntRange {
        return IntRange(
                start = min(this.start, other.start),
                stop = max(this.stop, other.stop)
        )
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntRange

        if (start != other.start) return false
        if (stop != other.stop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + stop
        return result
    }

}

/**
 * 文字列の範囲
 */
class TextRange(
        /**
         * 文字単位
         */
        @JsonProperty("char")
        val char: IntRange?,
        /**
         * 行単位
         */
        @JsonProperty("line")
        val line: IntRange
) {
    /**
     * 両者を含む最小の範囲を返す
     */
    fun include(other: TextRange): TextRange {
        return TextRange(
                char = other.char?.let { this.char?.include(it) },
                line = this.line.include(other.line)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextRange

        if (char != other.char) return false
        if (line != other.line) return false

        return true
    }

    override fun hashCode(): Int {
        var result = char.hashCode()
        result = 31 * result + line.hashCode()
        return result
    }
}

fun textRangeOf(charStart: Int, charStop: Int, lineStart: Int, lineStop: Int): TextRange {
    return TextRange(
            char = IntRange(charStart, charStop),
            line = IntRange(lineStart, lineStop)
    )
}

fun lineRangeOf(start: Int, stop: Int): TextRange {
    return TextRange(
            char = null,
            line = IntRange(start, stop)
    )
}