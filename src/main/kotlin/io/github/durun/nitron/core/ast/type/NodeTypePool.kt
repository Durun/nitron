package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType
import io.github.durun.nitron.util.filterIsInstance
import io.github.durun.nitron.util.toSparseList

class NodeTypePool private constructor(
		private val tokenTypeList: List<TokenType?>,
		private val tokenTypesRemain: Map<Int, TokenType>,
		private val ruleTypeList: List<RuleType?>
) {
	companion object {
		fun of(tokenTypes: Map<Int, TokenType>, ruleTypes: Map<Int, RuleType>, secondaryTokenTypes: Map<Int, TokenType> = emptyMap()): NodeTypePool {
			return NodeTypePool(
					tokenTypeList = tokenTypes.toSparseList(),
					tokenTypesRemain = secondaryTokenTypes,
					ruleTypeList = ruleTypes.toSparseList()
			)
		}

		fun of(types: Map<Int, NodeType>, secondaryTypes: Map<Int, NodeType> = emptyMap()): NodeTypePool {
			val tokens = types.filterIsInstance<Int, TokenType>()
			val rules = types.filterIsInstance<Int, RuleType>()
			val secondaryTokens = secondaryTypes.filterIsInstance<Int, TokenType>()
			return of(
					tokenTypes = tokens,
					ruleTypes = rules,
					secondaryTokenTypes = secondaryTokens
			)
		}
	}

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