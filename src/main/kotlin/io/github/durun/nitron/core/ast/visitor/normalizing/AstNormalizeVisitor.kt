package io.github.durun.nitron.core.ast.visitor.normalizing

import io.github.durun.nitron.core.InvalidTypeException
import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.type.NodeType
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.visitor.AstVisitor
import java.util.*
import kotlin.collections.HashMap

/**
 * @param [nonNumberedRuleMap] 番号を付けない正規化対応付け
 * @param [numberedRuleMap] 番号を付ける正規化対応付け
 */
@Deprecated("", ReplaceWith("astNormalizeVisitorOf(nonNumberedRuleMap = nonNumberedRuleMap, numberedRuleMap = numberedRuleMap, types = )"))
fun astNormalizeVisitorOf(
		nonNumberedRuleMap: Map<List<String>, String>,
		numberedRuleMap: Map<List<String>, String>): AstNormalizeVisitor {
	return StringAstNormalizeVisitor(nonNumberedRuleMap, numberedRuleMap)
}


fun astNormalizeVisitorOf(
        nonNumberedRuleMap: Map<List<String>, String>,
        numberedRuleMap: Map<List<String>, String>,
        types: NodeTypePool): AstNormalizeVisitor {
    return FastAstNormalizeVisitor(nonNumberedRuleMap, numberedRuleMap, types)
}

/**
 * [AstNode]にacceptさせるとトークンを正規化された[AstNode]を返すビジター
 */
abstract class AstNormalizeVisitor: AstVisitor<AstNode> {
    protected abstract fun enterTree(node: AstNode)
    protected abstract fun leaveTree(node: AstNode)
    protected abstract fun normalizeIfNeeded(node: AstNode): String?

    fun normalize(node: AstNode): AstNode {
        reset()
        return node.accept(this)
    }
    abstract fun reset()

    override fun visit(node: AstNode): AstNode {
        return node
    }

    override fun visitRule(node: AstRuleNode): AstNode {
        enterTree(node)
        val newText = normalizeIfNeeded(node)
        val newNode = newText
                ?.let {
                    NormalAstRuleNode(originalNode = node, text = newText)
                }
                ?: node.let {
                    val children = node.children.orEmpty().map { it.accept(this) }
                    node.replaceChildren(children)
                }
        leaveTree(node)
        return newNode
    }

    override fun visitTerminal(node: AstTerminalNode): AstNode {
        enterTree(node)
        val newToken = normalizeIfNeeded(node)
        val newNode = newToken
                ?.let { node.replaceToken(it) }
                ?: node
        leaveTree(node)
        return newNode
    }
}

private class StringAstNormalizeVisitor(
		private val nonNumberedRuleMap: Map<List<String>, String>,
		private val numberedRuleMap: Map<List<String>, String>
) : AstNormalizeVisitor() {
    // Map: (normalizedRuleName -> (id -> count))
    private val nameTables: Map<String, MutableMap<String, Int>> = numberedRuleMap.values.associateWith { HashMap() }

    private val visitedRuleStack: Stack<String> = Stack()

    override fun enterTree(node: AstNode) {
        visitedRuleStack.push(node.type.name)
    }

    override fun leaveTree(node: AstNode) {
        visitedRuleStack.pop()
    }

    override fun reset() {
        nameTables.forEach { (_, map) -> map.clear() }
        visitedRuleStack.clear()
    }

    override fun normalizeIfNeeded(node: AstNode): String? {
        val id = node
        val nonNumbered = nonNumberedRuleMap[visitedRuleStack]
        val numbered = numberedRuleMap[visitedRuleStack]?.let { "${it}${getAndUpdateRuleCount(it, id)}" }
        return nonNumbered ?: numbered
    }

    private fun getAndUpdateRuleCount(normalizedRuleName: String, id: String): Int {
        val idTable = nameTables[normalizedRuleName] ?: throw IllegalStateException("No such rule in nameTables")
        idTable.putIfAbsent(id, idTable.size)
        return idTable[id] ?: throw IllegalStateException("No such id in idTable")
    }
}


private class FastAstNormalizeVisitor private constructor(
        private val nonNumberedRuleMap: RuleMap,
        private val numberedRuleMap: RuleMap
): AstNormalizeVisitor() {
    constructor(nonNumberedRuleMap: Map<List<String>, String>,
                numberedRuleMap: Map<List<String>, String>, types: NodeTypePool): this(
            nonNumberedRuleMap = nonNumberedRuleMap.also{ _ ->
                val invalidTypes = (nonNumberedRuleMap.keys + numberedRuleMap.keys)
                        .flatten()
                        .filter { types.getType(it) == null }
                if (invalidTypes.isNotEmpty()) {
                    throw InvalidTypeException(invalidTypes)
                }
            }.mapKeys { (names, _) ->
                names.mapNotNull { name -> types.allTypes.find { it.name == name } }
            }.let { RuleMap(it) },
            numberedRuleMap = numberedRuleMap.mapKeys { (names, _) ->
                names.mapNotNull { name -> types.allTypes.find { it.name == name } }
            }.let { RuleMap(it) }
    )

    // Map: (normalizedRuleName -> (id -> count))
    private val nameTables: Map<String, MutableMap<String, Int>> = numberedRuleMap.values.associateWith { HashMap<String, Int>() }

    private val stack: Stack<NodeType> = Stack()

    override fun enterTree(node: AstNode) {
        stack.push(node.type)
    }

    override fun leaveTree(node: AstNode) {
        stack.pop()
    }

    override fun reset() {
        nameTables.forEach { (_, map) -> map.clear() }
        stack.clear()
    }

    override fun normalizeIfNeeded(node: AstNode): String? {
        val id = node.getText()
        val nonNumbered = nonNumberedRuleMap[stack]
        val numbered = numberedRuleMap[stack]?.let { "${it}${getAndUpdateRuleCount(it, id)}" }
        return nonNumbered ?: numbered
    }

    private fun getAndUpdateRuleCount(normalizedRuleName: String, id: String): Int {
        val idTable = nameTables[normalizedRuleName] ?: throw IllegalStateException("No such rule in nameTables")
        idTable.putIfAbsent(id, idTable.size)
        return idTable[id] ?: throw IllegalStateException("No such id in idTable")
    }

    /**
     * (正規化対象の規則, 正規化後の記号)の対応関係
     * @param [ruleMap] key=正規化対象の構造を 親->子孫 の順に格納したリスト, value=正帰化後の記号
     */
    private class RuleMap(
            private val ruleMap: Map<List<NodeType>, String>
    ): Map<List<NodeType>, String> by ruleMap {
        private val keyLengths = ruleMap.keys.map { it.size }.sorted().distinct()

        /**
         * [key]の後ろ側に対応する(正規化後の記号)を返す.
         * 複数該当する場合は最短マッチ.
         *
         * @param [key] 構文木の規則のスタック
         * @return 正規化後の記号. 該当するものが存在しなければnullを返す.
         */
        override fun get(key: List<NodeType>): String? {
            return keyLengths.mapNotNull { length ->
                val subStack = key.takeLast(length)
                ruleMap[subStack]
            }.firstOrNull()
        }
    }
}