package io.github.durun.nitron.inout.model.ast

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

class SerializableAst {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT,
            property = "t"
    )
    @JsonSubTypes(
            JsonSubTypes.Type(name = "l", value = NodeList::class),
            JsonSubTypes.Type(name = "t", value = TerminalNode::class),
            JsonSubTypes.Type(name = "r", value = RuleNode::class),
            JsonSubTypes.Type(name = "R", value = NormalizedRuleNode::class)
    )
    interface Node {
        @get:JsonIgnore
        val type: Int
        @get:JsonIgnore
        val text: String
    }

    interface NonTerminalNode : Node {
        @get:JsonIgnore
        val children: List<Node>
        override val text: String
            get() = this.children.joinToString(" ") { it.text }
    }

    class NodeList(
            private val data: List<Node>
    ) :
            NonTerminalNode,
            List<Node> by data {

        override val type: Int
            get() = -1  // TODO

        override val children: List<Node>
            get() = data

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NodeList

            if (data != other.data) return false

            return true
        }

        override fun hashCode(): Int {
            return data.hashCode()
        }
    }

    class TerminalNode(
            private val data: Map.Entry<Int, String>
    ) :
            Node,
            Map.Entry<Int, String> by data {

        constructor(type: Int, text: String) : this(Entry(type, text))

        override val type: Int
            get() = data.key
        override val text: String
            get() = data.value

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
            NonTerminalNode,
            Map.Entry<Int, List<Node>> by data {

        constructor(type: Int, children: List<Node>) : this(Entry(type, children))

        override val type: Int
            get() = data.key
        override val children: List<Node>
            get() = data.value

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
        override val text: String
            get() = data.value

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
}