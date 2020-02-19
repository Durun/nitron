package io.github.durun.nitron.analyze.query

import io.github.durun.nitron.analyze.Pattern

internal class PatternCapturingQuery<out R>(private val map: (Pattern) -> R) : AnalyzeQuery<R> {
    override fun analyze(pattern: Pattern): R = map(pattern)
}