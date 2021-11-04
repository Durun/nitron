package com.github.durun.nitron.core.ast.visitor

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.node.NormalAstRuleNode
import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import io.kotest.core.spec.style.FreeSpec

class AstXmlBuildVisitorTest : FreeSpec({
    fun nodeOf(type: RuleType, vararg children: AstNode) = BasicAstRuleNode.of(type, children.toMutableList())
    fun nodeOf(type: TokenType, text: String, line: Int) = AstTerminalNode.of(text, type, line)
    fun nodeOf(type: RuleType, text: String) = NormalAstRuleNode(type, text)

    "visit" {
        val rule = RuleType(1, "Rule")
        val token = TokenType(2, "Token")
        val children = arrayOf(
            nodeOf(token, "text1", 0),
            nodeOf(
                rule,
                nodeOf(token, "text2", 1),
                nodeOf(token, "text3", 2),
                nodeOf(
                    rule,
                    nodeOf(token, "text4", 3)
                ),
                nodeOf(rule, "normText")
            ),
            nodeOf(rule, "V"),
            nodeOf(token, "end", 9)
        )
        val tree = nodeOf(rule, *children)

        val xml = tree.accept(AstXmlBuildVisitor)
        println(xml)
    }
})
