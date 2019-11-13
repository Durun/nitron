package io.github.durun.nitron.inout.model.ast

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT,
        property = "t"
)
@JsonSubTypes(
        JsonSubTypes.Type(name = "t", value = TerminalNode::class),
        JsonSubTypes.Type(name = "r", value = RuleNode::class),
        JsonSubTypes.Type(name = "R", value = NormalizedRuleNode::class)
)
interface Node {
    @get:JsonIgnore
    val type: Int
}

class TerminalNode(
        private val data: Map.Entry<Int, String>
) :
        Node,
        Map.Entry<Int, String> by data {

    constructor(type: Int, text: String) : this(Entry(type, text))

    override val type: Int
        get() = data.key
    val text: String
        @JsonIgnore get() = data.value

    override fun toString(): String {
        return "{$type: $text}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TerminalNode

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }
}

class RuleNode(
        private val data: Map.Entry<Int, List<Node>>
) :
        Node,
        Map.Entry<Int, List<Node>> by data {

    constructor(type: Int, children: List<Node>) : this(Entry(type, children))

    override val type: Int
        get() = data.key
    val children: List<Node>
        @JsonIgnore get() = data.value

    override fun toString(): String {
        return "{$type: [${children.joinToString()}]}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RuleNode

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }
}

class NormalizedRuleNode(
        private val data: Map.Entry<Int, String>
) :
        Node,
        Map.Entry<Int, String> by data {

    constructor(type: Int, text: String) : this(Entry(type, text))

    override val type: Int
        get() = data.key
    val text: String
        @JsonIgnore get() = data.value

    override fun toString(): String {
        return "{$type: $text}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NormalizedRuleNode

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }
}


private class Entry<K, V>(
        override val key: K,
        override val value: V
) : Map.Entry<K, V>

data class HashIndexedNode(
        val node: Node,
        val hash: ByteArray,
        val grammar: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HashIndexedNode

        if (node != other.node) return false
        if (!hash.contentEquals(other.hash)) return false
        if (grammar != other.grammar) return false

        return true
    }

    override fun hashCode(): Int {
        var result = node.hashCode()
        result = 31 * result + hash.contentHashCode()
        result = 31 * result + grammar.hashCode()
        return result
    }
}