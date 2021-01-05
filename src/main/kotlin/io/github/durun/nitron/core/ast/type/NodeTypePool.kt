package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType


class NodeTypePool private constructor(
		val grammar: String,
		private val tokenTypeMap: Map<Int, TokenType>,
		private val ruleTypeMap: Map<Int, RuleType>,
		private val synonymTokenTypes: Map<Int, TokenType>
) {
	companion object {
		fun of(grammarName: String, tokenTypes: Collection<TokenType>, ruleTypes: Collection<RuleType>, synonymTokenTypes: Collection<TokenType> = emptySet()): NodeTypePool {
			checkOverlap(tokenTypes)
			checkOverlap(ruleTypes)
			checkSynonym(tokenTypes, synonymTokenTypes)
			return NodeTypePool(
					grammarName,
					tokenTypeMap = tokenTypes.associateBy { it.index },
					synonymTokenTypes = synonymTokenTypes.associateBy { it.index },
					ruleTypeMap = ruleTypes.associateBy { it.index }
			)
		}

		fun of(grammarName: String, types: Collection<NodeType>, synonymTypes: Collection<NodeType> = emptySet()): NodeTypePool {
			return of(
					grammarName,
					tokenTypes = types.filterIsInstance<TokenType>(),
					ruleTypes = types.filterIsInstance<RuleType>(),
					synonymTokenTypes = synonymTypes.filterIsInstance<TokenType>()
			)
		}

		/**
		 * NodeType.indexの重複がないことを確認する
		 */
		private fun checkOverlap(types: Collection<NodeType>) {
			val overlappedTypes = types
					.groupBy { it.index }
					.filter { (_, value) -> 2 <= value.size }
			if (overlappedTypes.isNotEmpty()) {
				throw IllegalArgumentException("""
					types overlap: {
					  ${overlappedTypes.entries.joinToString(",\n") { (index, types) -> "$index: ${types.map { it.name }}" }}
					}
				""".trimIndent())
			}
		}

		/**
		 * [synonyms]の全てのindexが、[mainTypes]にも存在していることを確認する
		 */
		private fun checkSynonym(mainTypes: Collection<NodeType>, synonyms: Collection<NodeType>) {
			val mainIndice = mainTypes.map { it.index }
			val invalidSynonyms = synonyms
					.groupBy { it.index }
					.filterNot { mainIndice.contains(it.key) }
			if (invalidSynonyms.isNotEmpty()) {
				throw IllegalArgumentException("""
					only synonyms exist: {
					  ${invalidSynonyms.entries.joinToString(",\n") { (index, types) -> "$index: ${types.map { it.name }}" }}
					}
				""".trimIndent())
			}
		}
	}

	val tokenTypes: Set<TokenType> by lazy { tokenTypeMap.values.toSet() + synonymTokenTypes.values }
	val ruleTypes: Set<RuleType> by lazy { ruleTypeMap.values.toSet() }
	val allTypes: Set<NodeType> by lazy { tokenTypes + ruleTypes }

	fun getTokenType(index: Int): TokenType? = tokenTypeMap[index]
	fun getRuleType(index: Int): RuleType? = ruleTypeMap[index]
	fun getTokenType(name: String): TokenType? = tokenTypes.find { it.name == name }
			?.let { tokenTypeMap[it.index] }

	fun getRuleType(name: String): RuleType? = ruleTypes.find { it.name == name }
	fun getType(name: String): NodeType? = getRuleType(name) ?: getTokenType(name)

	fun filterTypes(predicate: (NodeType) -> Boolean): NodeTypePool {
		return NodeTypePool(
				grammar,
				tokenTypeMap = tokenTypeMap.filterValues(predicate),
				ruleTypeMap = ruleTypeMap.filterValues(predicate),
				synonymTokenTypes = synonymTokenTypes.filterValues(predicate)
		)
	}
}