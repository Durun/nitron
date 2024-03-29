package com.github.durun.nitron.core.parser.antlr

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.*

/**
 * [AstNode]をビルドするための[ParseTreeVisitor].
 * [ParseTree]にacceptさせると[AstNode]を返す.
 * 変換結果が空の場合はnullを返す。(PHPの "emptyStatement_" の変換結果が空になることが確認されている)
 *
 * @param [parser] 文法規則の情報を持つ[Parser].
 */
class AstBuildVisitor(
    grammarName: String?,
    parser: Parser
) : ParseTreeVisitor<AstNode?> {
    val nodeTypes = nodeTypePoolOf(grammarName, parser)

    override fun visitChildren(node: RuleNode?): AstNode? {
        val children = node?.children?.mapNotNull { it.accept(this) }
            ?: throw Exception("RuleNode has no children.")
        if (children.isEmpty()) return null

        val ruleIndex = node.ruleContext?.ruleIndex
            ?: throw Exception("Rulenode has no ruleIndex")
        val rule = nodeTypes.getRuleType(ruleIndex) ?: throw NoSuchElementException("No such rule: index=$ruleIndex")
        return BasicAstRuleNode.of(
            type = rule,
            children = children
        )
    }

    override fun visitTerminal(node: TerminalNode?): AstNode? {
        val token = node?.text
            ?: return null
        val symbol = node.symbol
        val tokenType = nodeTypes.getTokenType(symbol.type)
            ?: throw NoSuchElementException("No such tokenType: index=${symbol.type}")
        return AstTerminalNode.of(
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