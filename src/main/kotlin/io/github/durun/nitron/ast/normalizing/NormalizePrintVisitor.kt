package io.github.durun.nitron.ast.normalizing

import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.AstVisitor
import io.github.durun.nitron.ast.basic.AstTerminalNode
import io.github.durun.nitron.ast.basic.AstRuleNode
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap

class NormalizePrintVisitor(
        val nonNumberedRuleMap: NormalizingRuleMap,
        val numberedRuleMap: NormalizingRuleMap
): AstVisitor<String> {
    // Map: (normalizedRuleName -> (id -> count))
    private val nameTables: Map<String, MutableMap<String, Int>> = numberedRuleMap.values.associateWith { HashMap<String, Int>() }

    private val visitedRuleStack: Stack<String> = Stack()

    override fun visit(node: AstNode): String {
        TODO()
    }

    override fun visitRule(node: AstRuleNode): String {
        visitedRuleStack.push(node.ruleName)    //  enter
        val rule = node.ruleName
        val id = node.getText() ?: ""
        val thisText = normalizeRuleIfNeeded(rule, id)
        val childrenText = node.children?.joinToString(" ") { it.accept(this) }
        visitedRuleStack.pop()                  // leave
        return thisText ?: childrenText ?: ""
    }

    override fun visitTerminal(node: AstTerminalNode): String {
        visitedRuleStack.push(node.tokenType)   //  enter
        val thisText = normalizeTokenIfNeeded(node.tokenType) ?: node.token
        visitedRuleStack.pop()                  // leave
        return thisText
    }

    private fun normalizeTokenIfNeeded(tokenType: String): String? {
        return nonNumberedRuleMap[visitedRuleStack]
    }
    private fun normalizeRuleIfNeeded(rule: String, id: String): String? {
        return nonNumberedRuleMap[visitedRuleStack]
                ?: numberedRuleMap[visitedRuleStack]?.let{ "${it}${getAndUpdateRuleCount(it, id)}" }
    }
    private fun getAndUpdateRuleCount(normalizedRuleName: String, id: String): Int {
        val idTable = nameTables[normalizedRuleName] ?: throw IllegalStateException("No such rule in nameTables")
        idTable.putIfAbsent(id, idTable.size)
        return idTable[id] ?: throw IllegalStateException("No such id in idTable")
    }
}