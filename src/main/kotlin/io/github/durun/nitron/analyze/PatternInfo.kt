package io.github.durun.nitron.analyze


interface PatternInfo {
    val name: String
    fun getInfoString(): String
}

class BooleanPatternInfo(
        override val name: String,
        val isPositive: Boolean
) : PatternInfo {
    override fun getInfoString(): String {
        return if (isPositive) name else ""
    }
}