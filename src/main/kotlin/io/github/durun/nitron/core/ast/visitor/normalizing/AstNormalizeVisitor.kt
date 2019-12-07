package io.github.durun.nitron.core.ast.visitor.normalizing

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.NormalAstRuleNode
import io.github.durun.nitron.core.ast.visitor.AstVisitor
import java.util.*
import kotlin.collections.HashMap

/**
 * @param [nonNumberedRuleMap] 番号を付けない正規化対応付け
 * @param [numberedRuleMap] 番号を付ける正規化対応付け
 */
fun astNormalizeVisitorOf(
        nonNumberedRuleMap: NormalizingRuleMap,
        numberedRuleMap: NormalizingRuleMap): AstNormalizeVisitor {
    return StringAstNormalizeVisitor(nonNumberedRuleMap, numberedRuleMap)
}


/**
 * [AstNode]にacceptさせるとトークンを正規化された[AstNode]を返すビジター
 */
abstract class AstNormalizeVisitor : AstVisitor<AstNode> {
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
        private val nonNumberedRuleMap: NormalizingRuleMap,
        private val numberedRuleMap: NormalizingRuleMap
) : AstNormalizeVisitor() {
    // Map: (normalizedRuleName -> (id -> count))
    private val nameTables: Map<String, MutableMap<String, Int>> = numberedRuleMap.values.associateWith { HashMap<String, Int>() }

    private val visitedRuleStack: Stack<String> = Stack()

    override fun enterTree(node: AstNode) {
        visitedRuleStack.push(node.type.name)
    }

    override fun leaveTree(node: AstNode) {
        visitedRuleStack.pop()
    }

    override fun reset() {
        visitedRuleStack.clear()
    }

    override fun normalizeIfNeeded(node: AstNode): String? {
        val id = node.getText().orEmpty()
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