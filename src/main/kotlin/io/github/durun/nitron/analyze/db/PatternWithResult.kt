package io.github.durun.nitron.analyze.db

import io.github.durun.nitron.analyze.AnalyzeQuery
import io.github.durun.nitron.analyze.Pattern
import io.github.durun.nitron.analyze.PatternInfo


class PatternWithResult(
        val pattern: Pattern,
        val infos: List<PatternInfo>
) {
    fun getInfoString(): String = infos.joinToString { it.getInfoString() }
}


fun <R : PatternInfo> Pattern.analyzeBy(queries: List<AnalyzeQuery<R>>): PatternWithResult {
    return PatternWithResult(pattern = this, infos = queries.map { it.analyze(this) })
}

fun <R : PatternInfo> Sequence<Pattern>.analyzeBy(queries: List<AnalyzeQuery<R>>): Sequence<PatternWithResult> {
    return this.map { it.analyzeBy(queries) }
}