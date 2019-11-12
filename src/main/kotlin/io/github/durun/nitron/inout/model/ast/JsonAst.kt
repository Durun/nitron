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
}


private class Entry<K, V>(
        override val key: K,
        override val value: V
) : Map.Entry<K, V>