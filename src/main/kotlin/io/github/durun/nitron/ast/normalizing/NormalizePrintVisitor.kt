package io.github.durun.nitron.ast.normalizing

import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.AstVisitor
import io.github.durun.nitron.ast.basic.AstTerminalNode
import io.github.durun.nitron.ast.basic.AstRuleNode
import java.lang.IllegalStateException

class NormalizePrintVisitor(
        val nonNumberedRuleMap: Map<String, String>,
        val numberedRuleMap: Map<String, String>
): AstVisitor<String> {
    // Map: (normalizedRuleName -> (id -> count))
    private val nameTables: Map<String, MutableMap<String, Int>> = numberedRuleMap.values.associateWith { HashMap<String, Int>() }

    override fun visit(node: AstNode): String {
        TODO()
    }

    override fun visitRule(node: AstRuleNode): String {
        val rule = node.ruleName
        val id = node.getText() ?: ""
        val thisText = normalizeRuleIfNeeded(rule, id)
        val childrenText = node.children?.joinToString(" ") { it.accept(this) }
        return thisText ?: childrenText ?: ""
    }

    override fun visitTerminal(node: AstTerminalNode): String {
        return node.token
    }

    private fun normalizeRuleIfNeeded(rule: String, id: String): String? {
        return nonNumberedRuleMap[rule]
                ?: numberedRuleMap[rule]?.let{ "${it}${getAndUpdateRuleCount(it, id)}" }
    }
    private fun getAndUpdateRuleCount(normalizedRuleName: String, id: String): Int {
        val idTable = nameTables[normalizedRuleName] ?: throw IllegalStateException("No such rule in nameTables")
        idTable.putIfAbsent(id, idTable.size)
        return idTable[id] ?: throw IllegalStateException("No such id in idTable")
    }
}