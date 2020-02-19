package io.github.durun.nitron.analyze.db

import io.github.durun.nitron.analyze.Pattern
import io.github.durun.nitron.analyze.message.PatternResultMessage
import io.github.durun.nitron.analyze.query.AnalyzeQuery


class PatternWithResult(
        val pattern: Pattern,
        val results: List<PatternResultMessage>
) {
    fun getInfoString(): String = results.joinToString { it.toString() }
}


fun Pattern.analyzeBy(queries: List<AnalyzeQuery<PatternResultMessage?>>): PatternWithResult? {
    val results = queries.mapNotNull { it.analyze(this) }
            .takeIf { it.isNotEmpty() }
    return results?.let { PatternWithResult(pattern = this, results = it) }
}

fun Sequence<Pattern>.analyzeBy(queries: List<AnalyzeQuery<PatternResultMessage?>>): Sequence<PatternWithResult> {
    return this.mapNotNull { it.analyzeBy(queries) }
}