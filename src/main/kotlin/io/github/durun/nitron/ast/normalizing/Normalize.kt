package io.github.durun.nitron.ast.normalizing

import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.ast.basic.BasicAstRuleNode

fun BasicAstRuleNode.normalize(): NormalAstRuleNode = NormalAstRuleNode(this)

fun AstNode.normalizeByRules(rules: Collection<String>): AstNode
        = if (this is BasicAstRuleNode && rules.contains(this.ruleName))
                this.normalize()
        else    this.mapChildren { it.normalizeByRules(rules) }