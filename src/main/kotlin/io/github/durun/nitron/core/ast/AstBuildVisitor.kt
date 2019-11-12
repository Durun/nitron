package io.github.durun.nitron.core.ast

import io.github.durun.nitron.core.antlr4util.children
import io.github.durun.nitron.core.ast.basic.AstNode
import io.github.durun.nitron.core.ast.basic.AstTerminalNode
import io.github.durun.nitron.core.ast.basic.BasicAstRuleNode
import io.github.durun.nitron.core.ast.basic.textRangeOf
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.*

/**
 * [AstNode]をビルドするための[ParseTreeVisitor].
 * [ParseTree]にacceptさせると[AstNode]を返す.
 *
 * @param [parser] 文法規則の情報を持つ[Parser].
 */
class AstBuildVisitor(
        private val parser: Parser
) : ParseTreeVisitor<AstNode> {
    private val tokenTypeMap: Map<Int, String> = parser.tokenTypeMap.entries.map { it.value to it.key }.toMap()

    override fun visitChildren(node: RuleNode?): AstNode {
        val children = node?.children?.map { it.accept(this) }
                ?: throw Exception("RuleNode has no children.")

        val ruleIndex = node.ruleContext?.ruleIndex
                ?: throw Exception("Rulenode has no ruleIndex")
        val ruleName = parser.ruleNames[ruleIndex]
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
                range = textRangeOf(
                        charStart = symbol.startIndex,
                        charStop = symbol.stopIndex,
                        lineStart = symbol.line,
                        lineStop = symbol.line
                )
        )
    }


    override fun visitErrorNode(node: ErrorNode?): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(tree: ParseTree?): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}