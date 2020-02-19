package io.github.durun.nitron.analyze.message

import io.github.durun.nitron.analyze.Pattern
import io.github.durun.nitron.analyze.query.AnalyzeQuery


infix fun String.means(query: AnalyzeQuery<Boolean>): AnalyzeQuery<PatternResultMessage?> {
    return BooleanResultMessageQuery(base = query, name = this)
}

private class BooleanPatternResultMessage(private val queryName: String) : PatternResultMessage {
    override fun toString(): String = queryName
}

private class BooleanResultMessageQuery(
        private val base: AnalyzeQuery<Boolean>,
        val name: String
) : AnalyzeQuery<PatternResultMessage?> {
    override fun analyze(pattern: Pattern): PatternResultMessage? {
        return base.analyze(pattern).toMessage(name)
    }

    private fun Boolean.toMessage(name: String): PatternResultMessage? {
        return takeIf { this }?.let { BooleanPatternResultMessage(name) }
    }
}