package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.util.SimpleEntry
import org.antlr.v4.runtime.Parser

internal fun AstBuildVisitor.nodeTypePoolOf(antlrParser: Parser): NodeTypePool {
    return NodeTypePool(antlrParser)
}

class NodeTypePool private constructor(tokenTypes: Map<String, Int>, ruleNames: Iterable<String>) {
    internal constructor(antlrParser: Parser) : this(
            tokenTypes = antlrParser.tokenTypeMap,
            ruleNames = antlrParser.ruleNames.asList()
    )

    private val tokenTypes: List<TokenType?>
    private val tokenTypesRemain: Map<Int, TokenType>
    private val rules: List<Rule> = ruleNames.mapIndexed { index, name -> Rule(index, name) }

    init {
        val types = tokenTypes.entries
                .groupBy { it.value }
                .mapValues { (_, entries) -> entries.map { it.key } }
                .mapValues { (_, synonims) ->
                    synonims.filterNot { it.contains('\'') }
                            .firstOrNull()
                            ?: synonims.first()
                }.toMutableMap()
        val max = tokenTypes.values.max()
        val range = max?.let { 0..it }
        this.tokenTypes = range?.map { index ->
            types[index]?.let { name ->
                types.remove(index)
                TokenType(index, name)
            }
        }.orEmpty()
        this.tokenTypesRemain = types.mapValues { TokenType(index = it.key, name = it.value) }
    }

    fun getTokenType(index: Int): TokenType? = tokenTypes.getOrNull(index) ?: tokenTypesRemain[index]
    fun getRule(index: Int): Rule? = rules.getOrNull(index)
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