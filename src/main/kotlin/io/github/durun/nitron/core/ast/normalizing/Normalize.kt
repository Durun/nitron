package io.github.durun.nitron.core.ast.normalizing

import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.basic.BasicAstRuleNode

fun BasicAstRuleNode.normalize(): NormalAstRuleNode = NormalAstRuleNode(this)

fun AstNode.normalizeByRules(rules: Collection<String>): AstNode {
    return if (this is BasicAstRuleNode && rules.contains(this.ruleName))
        this.normalize()
    else
        this.mapChildren { it.normalizeByRules(rules) }
}