package io.github.durun.nitron.ast.basic

import io.github.durun.nitron.antlr4util.children
import io.github.durun.nitron.ast.AstNode
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.*

class AstBuildVisitor(
        private val parser: Parser
) : ParseTreeVisitor<AstNode> {

    override fun visitChildren(node: RuleNode?): AstNode {
        val children = node?.children?.map { it.accept(this) }
                ?: throw Exception("RuleNode has no children.")

        val ruleIndex = node.ruleContext?.ruleIndex
                ?: throw Exception("Rulenode has no ruleIndex")
        val ruleName = parser.ruleNames[ruleIndex]
                ?: throw Exception("can't get ruleName")

        return AstRuleNode(
                ruleName = ruleName,
                children = children
        )
    }

    override fun visitTerminal(node: TerminalNode?): AstNode {
        val token = node?.text
                ?: throw Exception("TerminalNode has no text")
        return AstTerminalNode(
                token = token
        )
    }


    override fun visitErrorNode(node: ErrorNode?): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(tree: ParseTree?): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}