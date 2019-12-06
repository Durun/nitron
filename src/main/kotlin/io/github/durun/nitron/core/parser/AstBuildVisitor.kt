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
    private val tokenTypeMap: Map<Int, String> = TokenTypeBiMap(parser).fromIndex
    private val ruleNames: Array<String> = parser.ruleNames

    override fun visitChildren(node: RuleNode?): AstNode {
        val children = node?.children?.map { it.accept(this) }
                ?: throw Exception("RuleNode has no children.")

        val ruleIndex = node.ruleContext?.ruleIndex
                ?: throw Exception("Rulenode has no ruleIndex")
        val ruleName = ruleNames[ruleIndex]
                ?: throw Exception("can't get ruleName")
        return BasicAstRuleNode(
                ruleName = ruleName,
                children = children
        )
    }

    override fun visitTerminal(node: TerminalNode?): AstNode {
        val token = node?.text
                ?: throw Exception("TerminalNode has no text")
        val symbol = node.symbol
        val tokenType = tokenTypeMap[symbol.type] ?: throw NoSuchElementException("No such tokenType.")
        return AstTerminalNode(
                token = token,
                tokenType = tokenType,
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