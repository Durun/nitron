package io.github.durun.nitron.analyze

import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.core.ast.node.NodeTypePool


data class AnalyzeContext(
        val types: NodeTypePool
)


interface AnalyzeQuery<R : PatternInfo> {
    fun analyze(pattern: Pattern): R
}


abstract class AnalyzeQueryBase<R : PatternInfo> private constructor(
        private val types: NodeTypePool
) : AnalyzeQuery<R> {
    constructor(context: AnalyzeContext) : this(context.types)

    protected fun type(name: String): NodeType {
        return types.getType(name)
                ?: throw NoSuchElementException("Wrong declaration of ${this.javaClass}. No such NodeType: $name")
    }
}

