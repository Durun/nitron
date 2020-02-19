package io.github.durun.nitron.analyze.query

import io.github.durun.nitron.analyze.Pattern


infix fun AnalyzeQuery<Boolean>.and(other: AnalyzeQuery<Boolean>): AnalyzeQuery<Boolean> {
    return AndQuery(this, other)
}

infix fun AnalyzeQuery<Boolean>.or(other: AnalyzeQuery<Boolean>): AnalyzeQuery<Boolean> {
    return OrQuery(this, other)
}

fun AnalyzeQuery<Boolean>.not(): AnalyzeQuery<Boolean> {
    return NotQuery(this)
}

fun <T> AnalyzeQuery<T>.reversed(): AnalyzeQuery<T> {
    return ReversedQuery(this)
}

private class AndQuery(
        private val queries: Collection<AnalyzeQuery<Boolean>>
) : AnalyzeQuery<Boolean> {
    constructor(vararg queries: AnalyzeQuery<Boolean>) : this(queries.asList())

    override fun analyze(pattern: Pattern): Boolean {
        return queries.all { it.analyze(pattern) }
    }
}

private class OrQuery(
        private val queries: Collection<AnalyzeQuery<Boolean>>
) : AnalyzeQuery<Boolean> {
    constructor(vararg queries: AnalyzeQuery<Boolean>) : this(queries.asList())

    override fun analyze(pattern: Pattern): Boolean {
        return queries.any { it.analyze(pattern) }
    }
}

private class NotQuery(
        private val base: AnalyzeQuery<Boolean>
) : AnalyzeQuery<Boolean> {
    override fun analyze(pattern: Pattern): Boolean {
        return base.analyze(pattern).not()
    }
}

private class ReversedQuery<T>(
        private val base: AnalyzeQuery<T>
) : AnalyzeQuery<T> {
    override fun analyze(pattern: Pattern): T {
        return base.analyze(
                pattern = Pattern(
                        node = pattern.node.second to pattern.node.first,
                        hash = pattern.hash.second to pattern.hash.first
                )
        )
    }
}