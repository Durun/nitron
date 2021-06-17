package io.github.durun.nitron.core.parser.antlr

import io.github.durun.nitron.core.antlr4util.children
import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.type.nodeTypePoolOf
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.*

/**
 * [AstNode]をビルドするための[ParseTreeVisitor].
 * [ParseTree]にacceptさせると[AstNode]を返す.
 *
 * @param [parser] 文法規則の情報を持つ[Parser].
 */
class AstBuildVisitor(
        grammarName: String?,
        parser: Parser
) : ParseTreeVisitor<AstNode> {
    val nodeTypes = nodeTypePoolOf(grammarName, parser)

    override fun visitChildren(node: RuleNode?): AstNode {
        val children = node?.children?.map { it.accept(this) }
                ?: throw Exception("RuleNode has no children.")

        val ruleIndex = node.ruleContext?.ruleIndex
                ?: throw Exception("Rulenode has no ruleIndex")
        val rule = nodeTypes.getRuleType(ruleIndex) ?: throw NoSuchElementException("No such rule: index=$ruleIndex")
        return BasicAstRuleNode(
                type = rule,
                children = children.toMutableList()
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