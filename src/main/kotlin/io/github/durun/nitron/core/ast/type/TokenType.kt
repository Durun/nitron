package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.util.SimpleEntry

class TokenType private constructor(
        private val entry: Map.Entry<Int, String>
) : NodeType,
        Map.Entry<Int, String> by entry {

    constructor(index: Int, name: String) : this(SimpleEntry(index, name))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is TokenType) && (key == other.key)
    }

    override fun hashCode(): Int = key
    override fun toString(): String  = entry.toString()
}