package io.github.durun.nitron.inout.model.ast

interface Visitor<R> {
    fun visitTerminal(node: SerializableAst.TerminalNode): R
    fun visitRule(node: SerializableAst.RuleNode): R
    fun visitNormalizedRule(node: SerializableAst.NormalizedRuleNode): R
    fun visitNodeList(node: SerializableAst.NodeList): R
}