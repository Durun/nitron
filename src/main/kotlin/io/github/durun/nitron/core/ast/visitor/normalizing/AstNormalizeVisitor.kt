package io.github.durun.nitron.core.ast.visitor.normalizing

import io.github.durun.nitron.core.ast.basic.AstNode
import io.github.durun.nitron.core.ast.basic.AstRuleNode
import io.github.durun.nitron.core.ast.basic.AstTerminalNode
import io.github.durun.nitron.core.ast.basic.BasicAstRuleNode
import io.github.durun.nitron.core.ast.basic.NormalAstRuleNode
import io.github.durun.nitron.core.ast.visitor.AstVisitor
import java.util.*
import kotlin.collections.HashMap

/**
 * [AstNode]にacceptさせるとトークンを正規化された[AstNode]を返すビジター
 *
 * @param [nonNumberedRuleMap] 番号を付けない正規化対応付け
 * @param [numberedRuleMap] 番号を付ける正規化対応付け
 */
class AstNormalizeVisitor(
        private val nonNumberedRuleMap: NormalizingRuleMap,
        private val numberedRuleMap: NormalizingRuleMap
) : AstVisitor<AstNode> {
    // Map: (normalizedRuleName -> (id -> count))
    private val nameTables: Map<String, MutableMap<String, Int>> = numberedRuleMap.values.associateWith { HashMap<String, Int>() }

    private val visitedRuleStack: Stack<String> = Stack()

    override fun visit(node: AstNode): AstNode {
        return node
    }

    override fun visitRule(node: AstRuleNode): AstNode {
        visitedRuleStack.push(node.ruleName)    //  enter
        val newText = node.normalizeIfNeeded(visitedRuleStack)
        val newNode = newText
                ?.let {
                    NormalAstRuleNode(originalNode = node, text = newText)
                }
                ?: node.let {
                    val children = node.children.orEmpty().map { it.accept(this) }
                    BasicAstRuleNode(ruleName = it.ruleName, children = children)
                }
        visitedRuleStack.pop()                  // leave
        return newNode
    }

    override fun visitTerminal(node: AstTerminalNode): AstNode {
        visitedRuleStack.push(node.tokenType)   //  enter
        val newToken = node.normalizeIfNeeded(visitedRuleStack)
        val newNode = newToken
                ?.let {
                    AstTerminalNode(token = it, tokenType = node.tokenType, range = node.range)
                }
                ?: node
        visitedRuleStack.pop()                  // leave
        return newNode
    }

    private fun AstNode.normalizeIfNeeded(visitingStack: Stack<String>): String? {
        val id = this.getText().orEmpty()
        val nonNumbered = nonNumberedRuleMap[visitingStack]
        val numbered = numberedRuleMap[visitingStack]?.let { "${it}${getAndUpdateRuleCount(it, id)}" }
        return nonNumbered ?: numbered
    }

    private fun getAndUpdateRuleCount(normalizedRuleName: String, id: String): Int {
        val idTable = nameTables[normalizedRuleName] ?: throw IllegalStateException("No such rule in nameTables")
        idTable.putIfAbsent(id, idTable.size)
        return idTable[id] ?: throw IllegalStateException("No such id in idTable")
    }
}