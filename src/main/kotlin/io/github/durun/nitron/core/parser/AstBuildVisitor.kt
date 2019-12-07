package io.github.durun.nitron.core.parser

import io.github.durun.nitron.core.antlr4util.children
import io.github.durun.nitron.core.ast.node.*
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.*

/**
 * [AstNode]をビルドするための[ParseTreeVisitor].
 * [ParseTree]にacceptさせると[AstNode]を返す.
 *
 * @param [parser] 文法規則の情報を持つ[Parser].
 */
class AstBuildVisitor(
        parser: Parser
) : ParseTreeVisitor<AstNode> {
    val nodeTypes = nodeTypePoolOf(parser)

    override fun visitChildren(node: RuleNode?): AstNode {
        val children = node?.children?.map { it.accept(this) }
                ?: throw Exception("RuleNode has no children.")

        val ruleIndex = node.ruleContext?.ruleIndex
                ?: throw Exception("Rulenode has no ruleIndex")
        val rule = nodeTypes.getRule(ruleIndex) ?: throw NoSuchElementException("No such rule: index=$ruleIndex")
        return BasicAstRuleNode(
                type = rule,
                children = children
        )
    }

    override fun visitTerminal(node: TerminalNode?): AstNode {
        val token = node?.text
                ?: throw Exception("TerminalNode has no text")
        val symbol = node.symbol
        val tokenType = nodeTypes.getTokenType(symbol.type)
                ?: throw NoSuchElementException("No such tokenType: index=${symbol.type}")
        return AstTerminalNode(
                type = tokenType,
                token = token,
                line = symbol.line
        )
    }


    override fun visitErrorNode(node: ErrorNode?): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(tree: ParseTree?): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}