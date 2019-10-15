package io.github.durun.nitron.ast.basic

import com.fasterxml.jackson.annotation.JsonProperty

class TextRange(
        @JsonProperty("start")
        val start: Int,
        @JsonProperty("stop")
        val stop: Int
) {
        fun contains(that: TextRange): Boolean
                = startsBeforeOrJust(that) && endsAfterOrJust(that)

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
}