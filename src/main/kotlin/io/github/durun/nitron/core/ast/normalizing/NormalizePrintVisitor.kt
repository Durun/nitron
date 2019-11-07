package io.github.durun.nitron.core.ast.normalizing

import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.AstVisitor
import io.github.durun.nitron.core.ast.basic.AstRuleNode
import io.github.durun.nitron.core.ast.basic.AstTerminalNode
import java.util.*
import kotlin.collections.HashMap

/**
 * [AstNode]にacceptさせると正規化されたソースコードを返すビジター
 *
 * @param [nonNumberedRuleMap] 番号を付けない正規化対応付け
 * @param [numberedRuleMap] 番号を付ける正規化対応付け
 */
class NormalizePrintVisitor(
        val nonNumberedRuleMap: NormalizingRuleMap,
        val numberedRuleMap: NormalizingRuleMap
) : AstVisitor<String> {
    // Map: (normalizedRuleName -> (id -> count))
    private val nameTables: Map<String, MutableMap<String, Int>> = numberedRuleMap.values.associateWith { HashMap<String, Int>() }

    private val visitedRuleStack: Stack<String> = Stack()

    override fun visit(node: AstNode): String {
        return node.getText().orEmpty()
    }

    override fun visitRule(node: AstRuleNode): String {
        visitedRuleStack.push(node.ruleName)    //  enter
        val id = node.getText() ?: ""
        val thisText = normalizeVisitingNodeIfNeeded(id)
        val childrenText = node.children?.joinToString(" ") { it.accept(this) }
        visitedRuleStack.pop()                  // leave
        return thisText ?: childrenText ?: ""
    }

    override fun visitTerminal(node: AstTerminalNode): String {
        visitedRuleStack.push(node.tokenType)   //  enter
        val thisText = normalizeVisitingNodeIfNeeded(node.token) ?: node.token
        visitedRuleStack.pop()                  // leave
        return thisText
    }

    private fun normalizeVisitingNodeIfNeeded(id: String): String? {
        return nonNumberedRuleMap[visitedRuleStack]
                ?: numberedRuleMap[visitedRuleStack]?.let { "${it}${getAndUpdateRuleCount(it, id)}" }
    }

    private fun getAndUpdateRuleCount(normalizedRuleName: String, id: String): Int {
        val idTable = nameTables[normalizedRuleName] ?: throw IllegalStateException("No such rule in nameTables")
        idTable.putIfAbsent(id, idTable.size)
        return idTable[id] ?: throw IllegalStateException("No such id in idTable")
    }
}