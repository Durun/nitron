package io.github.durun.nitron.analyze.contexts

import io.github.durun.nitron.analyze.AnalyzeContext
import io.github.durun.nitron.analyze.Pattern
import io.github.durun.nitron.analyze.query.AnalyzeQuery
import io.github.durun.nitron.analyze.query.PatternCapturingQuery
import io.github.durun.nitron.inout.model.ast.SerializableAst


fun <T> AnalyzeContext.ifPattern(condition: (Pattern) -> T): AnalyzeQuery<T> {
    return PatternCapturingQuery(condition)
}

fun <T> AnalyzeContext.ifBefore(condition: (SerializableAst.Node) -> T): AnalyzeQuery<T> {
    return this.ifPattern { condition(it.node.first) }
}

fun <T> AnalyzeContext.ifAfter(condition: (SerializableAst.Node) -> T): AnalyzeQuery<T> {
    return this.ifPattern { condition(it.node.second) }
}