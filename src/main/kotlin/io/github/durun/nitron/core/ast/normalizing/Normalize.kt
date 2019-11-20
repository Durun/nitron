package io.github.durun.nitron.core.ast.normalizing

import io.github.durun.nitron.core.ast.basic.BasicAstRuleNode

fun BasicAstRuleNode.normalize(): NormalAstRuleNode = NormalAstRuleNode(this)