package com.github.durun.nitron.core.ast.visitor

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class AstLineGetVisitorTest : FreeSpec({
    fun nodeOf(type: RuleType, vararg children: AstNode) = BasicAstRuleNode.of(type, children.toList())
    fun nodeOf(type: TokenType, text: String, line: Int) = AstTerminalNode.of(text, type, line)

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
                nodeOf(token, "text5", 4)
            ),
            nodeOf(token, "end", 5)
        )
        val tree = nodeOf(rule, *children)

        tree.accept(AstLineGetVisitor) shouldBe LineRange(0, 5)
        tree.children[0].accept(AstLineGetVisitor) shouldBe LineRange(0, 0)
        tree.children[1].accept(AstLineGetVisitor) shouldBe LineRange(1, 4)
    }
})
