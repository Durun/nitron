package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.util.SimpleEntry
import org.antlr.v4.runtime.Parser

internal fun AstBuildVisitor.nodeTypePoolOf(antlrParser: Parser): NodeTypePool {
    return NodeTypePool(antlrParser)
}

class NodeTypePool private constructor(
        private val tokenTypeList: List<TokenType?>,
        private val tokenTypesRemain: Map<Int, TokenType>,
        private val ruleList: List<Rule>
) {
    internal constructor(antlrParser: Parser) : this(
            tokenTypeMap = antlrParser.tokenTypeMap,
            ruleNames = antlrParser.ruleNames.asList()
    )

    private constructor(tokenTypeMap: Map<String, Int>, ruleNames: Iterable<String>) : this(
            tokenTypeMap = tokenTypeMap
                    .entries
                    .groupBy { it.value }
                    .mapValues { (_, entries) -> entries.map { it.key } }
                    .mapValues { (_, synonyms) ->
                        synonyms.filterNot { it.contains('\'') }
                                .firstOrNull()
                                ?: synonyms.first()
                    }
                    .mapValues { (index, name) -> TokenType(index, name) },
            rules = ruleNames.mapIndexed { index, name -> Rule(index, name) }
    )

    constructor(tokenTypes: Iterable<String>, ruleNames: Iterable<String>) : this(
            tokenTypeList = tokenTypes.mapIndexed { index, it -> TokenType(index, it) }.toList(),
            tokenTypesRemain = emptyMap(),
            ruleList = ruleNames.mapIndexed { index, it -> Rule(index, it) }.toList()
    )

    private constructor(tokenTypeMap: Map<Int, TokenType>, rules: List<Rule>) : this(
            tokenTypeList = tokenTypeMap
                    .let { typeMap ->
                        val max = tokenTypeMap.keys.max()
                        val range = max?.let { 0..it }
                        range?.map { index -> typeMap[index] }
                                .orEmpty()
                    },
            tokenTypesRemain = tokenTypeMap.filterKeys { it < 0 },
            ruleList = rules
    )

    val tokenTypes: Set<TokenType> by lazy { tokenTypeList.filterNotNull().toSet() + tokenTypesRemain.values }
    val rules: Set<Rule> by lazy { ruleList.toSet() }
    val allTypes: Set<NodeType> by lazy { tokenTypes + rules }

    fun getTokenType(index: Int): TokenType? = tokenTypeList.getOrNull(index) ?: tokenTypesRemain[index]
    fun getRule(index: Int): Rule? = ruleList.getOrNull(index)
    fun getTokenType(name: String): TokenType? = tokenTypes.find { it.name == name }
    fun getRule(name: String): Rule? = rules.find { it.name == name }
    fun getType(name: String): NodeType? = getRule(name) ?: getTokenType(name)

    fun filterRulesAndTokenTypes(remainRules: List<String>): NodeTypePool {
        return NodeTypePool(
                tokenTypeList = tokenTypeList.map { if (remainRules.contains(it?.name)) it else null },
                tokenTypesRemain = tokenTypesRemain.filterValues { remainRules.contains(it.name) },
                ruleList = ruleList.filter { remainRules.contains(it.name) }
        )
    }
}


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