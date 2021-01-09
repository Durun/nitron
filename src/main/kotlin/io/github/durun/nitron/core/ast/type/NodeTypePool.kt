package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.ast.node.NodeType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable(with = NodeTypePoolSerializer::class)
class NodeTypePool private constructor(
		val grammar: String,
		private val tokenTypeMap: Map<Int, TokenType>,
		private val ruleTypeMap: Map<Int, RuleType>,
		private val synonymTokenTypes: Map<Int, TokenType>
) {
	companion object {
		val EMPTY: NodeTypePool = of("", emptySet())
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

		fun of(grammarName: String, tokenTypes: Map<Int, String>, ruleTypes: Map<Int, String>, synonymTokenTypes: Map<String, Int>): NodeTypePool {
			return of(
					grammarName,
					tokenTypes = tokenTypes.entries.map { (index, name) -> TokenType(index, name) },
					ruleTypes = ruleTypes.entries.map { (index, name) -> RuleType(index, name) },
					synonymTokenTypes = synonymTokenTypes.entries.map { (name, index) -> TokenType(index, name) }
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

	val mainTokenTypes: Set<TokenType> by lazy { tokenTypeMap.values.toSet() }
	val tokenTypes: Set<TokenType> by lazy { mainTokenTypes + synonymTokenTypes.values }
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

	override fun toString(): String {
		return "NodeTypePool@${Json.encodeToString(this)}"
	}
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as NodeTypePool

		if (grammar != other.grammar) return false
		if (!tokenTypeMap.entries.containsAll(other.tokenTypeMap.entries)) return false
		if (!other.tokenTypeMap.entries.containsAll(tokenTypeMap.entries)) return false
		if (!ruleTypeMap.entries.containsAll(other.ruleTypeMap.entries)) return false
		if (!other.ruleTypeMap.entries.containsAll(ruleTypeMap.entries)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = grammar.hashCode()
		result = 31 * result + tokenTypeMap.hashCode()
		result = 31 * result + ruleTypeMap.hashCode()
		return result
	}
}