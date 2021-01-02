package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.util.filterIsInstance
import io.github.durun.nitron.util.toSparseList
import org.antlr.v4.runtime.Parser

class NodeTypePool private constructor(
		private val tokenTypeList: List<TokenType?>,
		private val tokenTypesRemain: Map<Int, TokenType>,
		private val ruleTypeList: List<RuleType?>
) {
	companion object {
		fun of(types: Map<Int, NodeType>, secondaryTypes: Map<Int, NodeType> = emptyMap()): NodeTypePool {
			val tokens = types.filterIsInstance<Int, TokenType>()
			val rules = types.filterIsInstance<Int, RuleType>()
			val secondaryTokens = secondaryTypes.filterIsInstance<Int, TokenType>()
			return NodeTypePool(
					tokenTypeList = tokens.toSparseList(),
					tokenTypesRemain = secondaryTokens,
					ruleTypeList = rules.toSparseList()
			)
		}
	}

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
            ruleTypes = ruleNames.mapIndexed { index, name -> RuleType(index, name) }
    )

    constructor(tokenTypes: Iterable<String>, ruleNames: Iterable<String>) : this(
            tokenTypeList = tokenTypes.mapIndexed { index, it -> TokenType(index, it) }.toList(),
            tokenTypesRemain = emptyMap(),
            ruleTypeList = ruleNames.mapIndexed { index, it -> RuleType(index, it) }.toList()
    )

    private constructor(tokenTypeMap: Map<Int, TokenType>, ruleTypes: List<RuleType>) : this(
            tokenTypeList = tokenTypeMap
                    .let { typeMap ->
                        val max = tokenTypeMap.keys.max()
                        val range = max?.let { 0..it }
                        range?.map { index -> typeMap[index] }
                                .orEmpty()
                    },
            tokenTypesRemain = tokenTypeMap.filterKeys { it < 0 },
            ruleTypeList = ruleTypes
    )

    val tokenTypes: Set<TokenType> by lazy { tokenTypeList.filterNotNull().toSet() + tokenTypesRemain.values }
	val ruleTypes: Set<RuleType> by lazy { ruleTypeList.filterNotNull().toSet() }
    val allTypes: Set<NodeType> by lazy { tokenTypes + ruleTypes }

    fun getTokenType(index: Int): TokenType? = tokenTypeList.getOrNull(index) ?: tokenTypesRemain[index]
    fun getRuleType(index: Int): RuleType? = ruleTypeList.getOrNull(index)
    fun getTokenType(name: String): TokenType? = tokenTypes.find { it.name == name }
    fun getRuleType(name: String): RuleType? = ruleTypes.find { it.name == name }
    fun getType(name: String): NodeType? = getRuleType(name) ?: getTokenType(name)

    fun filterRulesAndTokenTypes(remainRules: List<String>): NodeTypePool {
        return NodeTypePool(
				tokenTypeList = tokenTypeList.map { if (remainRules.contains(it?.name)) it else null },
				tokenTypesRemain = tokenTypesRemain.filterValues { remainRules.contains(it.name) },
				ruleTypeList = ruleTypeList.filterNotNull().filter { remainRules.contains(it.name) }
		)
    }
}