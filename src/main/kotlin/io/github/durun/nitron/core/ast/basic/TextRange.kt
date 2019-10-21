package io.github.durun.nitron.core.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 文字列の範囲
 */
class TextRange(
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
    fun contains(that: TextRange): Boolean {
        return startsBeforeOrJust(that) && endsAfterOrJust(that)
    }

    override fun equals(other: Any?): Boolean {
        return (other is TextRange) && this.equals(other)
    }

    fun equals(other: TextRange?): Boolean {
        return other?.let {
            (this.start == it.start) && (this.stop == it.stop)
        } ?: false
    }

    private fun startsBeforeOrJust(that: TextRange): Boolean {
        return (this.start <= that.start)
    }

    private fun endsAfterOrJust(that: TextRange): Boolean {
        return (that.stop <= this.stop)
    }

    override fun hashCode(): Int {
        return arrayOf(
                start,
                stop
        ).hashCode()
    }
}