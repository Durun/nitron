package io.github.durun.nitron.analyze.contexts

import io.github.durun.nitron.analyze.AnalyzeContext
import io.github.durun.nitron.analyze.node.contains
import io.github.durun.nitron.analyze.node.hasNestTypeOf
import io.github.durun.nitron.analyze.query.AnalyzeQuery
import io.github.durun.nitron.analyze.query.and
import io.github.durun.nitron.analyze.query.not
import io.github.durun.nitron.inout.model.ast.SerializableAst


fun AnalyzeContext.ifBeforeContains(type: String): AnalyzeQuery<Boolean> {
    return this.ifBefore { it.contains(typeOf(type)) }
}

fun AnalyzeContext.ifAfterContains(type: String): AnalyzeQuery<Boolean> {
    return this.ifAfter { it.contains(typeOf(type)) }
}

fun AnalyzeContext.ifChanged(type: Pair<String, String>): AnalyzeQuery<Boolean> {
    return this.ifBeforeContains(type.first) and this.ifAfterContains(type.second)
}

fun AnalyzeContext.ifIntroduced(type: String): AnalyzeQuery<Boolean> {
    return this.ifBeforeContains(type).not() and this.ifAfterContains(type)
}

fun AnalyzeContext.ifRemoved(type: String): AnalyzeQuery<Boolean> {
    return this.ifBeforeContains(type) and this.ifAfterContains(type).not()
}


fun AnalyzeContext.ifBeforeContains(vararg nest: String): AnalyzeQuery<Boolean> {
    val types = nest.map { typeOf(it) }.toTypedArray()
    return this.ifAfter { it.hasNestTypeOf(*types) }
}


fun AnalyzeContext.ifAfterContains(vararg nest: String): AnalyzeQuery<Boolean> {
    val types = nest.map { typeOf(it) }.toTypedArray()
    return this.ifAfter { it.hasNestTypeOf(*types) }
}

fun AnalyzeContext.ifIntroduced(vararg nest: String): AnalyzeQuery<Boolean> {
    return this.ifBeforeContains(*nest).not() and this.ifAfterContains(*nest)
}

fun AnalyzeContext.ifRemoved(vararg nest: String): AnalyzeQuery<Boolean> {
    return this.ifBeforeContains(*nest) and this.ifAfterContains(*nest).not()
}

fun AnalyzeContext.ifChanged(
        before: (SerializableAst.Node) -> Boolean,
        after: (SerializableAst.Node) -> Boolean): AnalyzeQuery<Boolean> {
    return this.ifBefore(before) and this.ifAfter(after)
}

fun AnalyzeContext.ifIntroduced(cond: (SerializableAst.Node) -> Boolean): AnalyzeQuery<Boolean> {
    return this.ifBefore(cond).not() and this.ifAfter(cond)
}

fun AnalyzeContext.ifRemoved(cond: (SerializableAst.Node) -> Boolean): AnalyzeQuery<Boolean> {
    return this.ifBefore(cond) and this.ifAfter(cond).not()
}

fun AnalyzeContext.typeIs(name: String): (SerializableAst.Node) -> Boolean {
    return { it.type == this.typeOf(name).index }
}

infix fun <T> ((T) -> Boolean).and(other: ((T) -> Boolean)): ((T) -> Boolean) {
    return { this(it) && other(it) }
}

infix fun <T> ((T) -> Boolean).or(other: ((T) -> Boolean)): ((T) -> Boolean) {
    return { this(it) || other(it) }
}

fun <T> ((T) -> Boolean).not(): ((T) -> Boolean) {
    return { !this(it) }
}
