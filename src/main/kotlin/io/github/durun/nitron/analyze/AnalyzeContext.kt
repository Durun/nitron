package io.github.durun.nitron.analyze

import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.core.ast.node.NodeTypePool


data class AnalyzeContext(
        val types: NodeTypePool
) {
    fun <T : Any> scope(block: AnalyzeContext.() -> T): T {
        return this.block()
    }

    internal fun typeOf(name: String): NodeType {
        return types.getType(name)
                ?: throw NoSuchElementException("Wrong declaration of ${this.javaClass}. No such NodeType: $name")
    }

    internal fun typesOf(vararg name: String): List<NodeType> = name.map { typeOf(it) }
}