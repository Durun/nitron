package io.github.durun.nitron.inout.model.ast

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.durun.nitron.core.ast.node.NodeTypePool
import io.github.durun.nitron.core.parser.TokenTypeBiMap
import org.antlr.v4.runtime.Parser

fun NodeTypePool.toSerializable(grammarName: String): NodeTypeSet {
    return NodeTypeSet(grammarName, types = this)
}

/**
 * 文法[grammar]が持つtokenType, ruleNameの集合
 */
class NodeTypeSet internal constructor(
        @JsonProperty
        val grammar: String,
        @JsonProperty
        val tokenTypes: ArrayMap<String>,
        @JsonProperty
        val ruleNames: ArrayMap<String>
) {
    @Deprecated("use NodeTypePool", ReplaceWith("this(grammarName = grammarName, types = NodeTypePool(parser))"))
    constructor(grammarName: String, parser: Parser) : this(
            tokenTypes = parser.toTokenTypeMap(),
            ruleNames = ArrayMap(parser.ruleNames),
            grammar = grammarName
    )

    constructor(grammarName: String, types: NodeTypePool) : this(
            grammar = grammarName,
            tokenTypes = ArrayMap(types.tokenTypes.map { it.name }.toTypedArray()),
            ruleNames = ArrayMap(types.rules.map { it.name }.toTypedArray())
    )

    fun toNodeTypePool(): NodeTypePool = NodeTypePool(
            tokenTypes = tokenTypes.array.asIterable(),
            ruleNames = ruleNames.array.asIterable()
    )

    /**
     * @return 添字[index]に対応するtokenType
     */
    fun token(index: Int): String? = tokenTypes.array[index]

    /**
     * @return tokenType[type]に対応する添字
     */
    fun token(type: String): Int? = tokenTypes.map[type]

    /**
     * @return 添字[index]に対応するruleName
     */
    fun rule(index: Int): String? = ruleNames.array[index]

    /**
     * @return ruleName[name]に対応する添字
     */
    fun rule(name: String): Int? = ruleNames.map[name]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeTypeSet

        if (grammar != other.grammar) return false
        if (tokenTypes != other.tokenTypes) return false
        if (ruleNames != other.ruleNames) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grammar.hashCode()
        result = 31 * result + tokenTypes.hashCode()
        result = 31 * result + ruleNames.hashCode()
        return result
    }
}

class ArrayMap<T>(
        @JsonProperty
        val array: Array<T>
) {
    @JsonIgnore
    val map: Map<T, Int> = array.associateBy({ it }, { array.indexOf(it) })

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayMap<*>

        if (!array.contentEquals(other.array)) return false

        return true
    }

    override fun hashCode(): Int {
        return array.contentHashCode()
    }
}


private fun Parser.toTokenTypeMap(): ArrayMap<String> {
    val list = TokenTypeBiMap(this)
            .fromIndex
            .toSortedMap()
            .map { it.value }
            .toList()
    return ArrayMap(list.toTypedArray())
}