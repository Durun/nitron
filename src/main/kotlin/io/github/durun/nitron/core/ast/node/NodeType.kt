package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.util.SimpleEntry
import org.antlr.v4.runtime.Parser

internal fun AstBuildVisitor.nodeTypePoolOf(antlrParser: Parser): NodeTypePool {
    return NodeTypePool(antlrParser)
}

class NodeTypePool private constructor(
        private val tokenTypes: List<TokenType?>,
        private val tokenTypesRemain: Map<Int, TokenType>,
        private val rules: List<Rule>
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

    private constructor(tokenTypeMap: Map<Int, TokenType>, rules: List<Rule>) : this(
            tokenTypes = tokenTypeMap
                    .let { typeMap ->
                        val max = tokenTypeMap.keys.max()
                        val range = max?.let { 0..it }
                        range?.map { index -> typeMap[index] }
                                .orEmpty()
                    },
            tokenTypesRemain = tokenTypeMap.filterKeys { it < 0 },
            rules = rules
    )


    fun getTokenType(index: Int): TokenType? = tokenTypes.getOrNull(index) ?: tokenTypesRemain[index]
    fun getRule(index: Int): Rule? = rules.getOrNull(index)

    fun filterRulesAndTokenTypes(remainRules: List<String>): NodeTypePool {
        return NodeTypePool(
                tokenTypes = tokenTypes.map { if (remainRules.contains(it?.name)) it else null },
                tokenTypesRemain = tokenTypesRemain.filterValues { remainRules.contains(it.name) },
                rules = rules.filter { remainRules.contains(it.name) }
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