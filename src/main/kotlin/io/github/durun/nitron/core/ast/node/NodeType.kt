package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.util.SimpleEntry

interface NodeType : Map.Entry<Int, String> {
    val index: Int
        get() = this.key
    val name: String
        get() = this.value
}

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
}

class Rule private constructor(
        private val entry: Map.Entry<Int, String>
) : NodeType,
        Map.Entry<Int, String> by entry {

    constructor(index: Int, name: String) : this(SimpleEntry(index, name))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is Rule) && (key == other.key)
    }

    override fun hashCode(): Int = key
}