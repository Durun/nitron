package com.github.durun.nitron.testutil

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import com.github.durun.nitron.core.ast.visitor.AstPrintVisitor


fun astNode(text: String, type: TokenType, line: Int): AstTerminalNode = AstTerminalNode(text, type, line)
fun astNode(type: RuleType, body: NodeBuilderScope.() -> Unit): AstRuleNode {
    val builder = NodeBuilderScope()
    builder.body()
    return BasicAstRuleNode(type, builder.build().toMutableList())
}

class NodeBuilderScope(startLine: Int = 1) {
    private var lineCache = startLine
    private val children: MutableList<AstNode> = mutableListOf()
    fun build(): List<AstNode> = children

    fun token(text: String, type: TokenType, line: Int = lineCache) {
        children += AstTerminalNode(text, type, line)
        lineCache = line
    }

    fun node(type: RuleType, body: NodeBuilderScope.() -> Unit) {
        val builder = NodeBuilderScope(lineCache)
        builder.body()
        children += BasicAstRuleNode(type, builder.build().toMutableList())
    }
}

/**
 * Usage
 */
fun main() {
    val token = TokenType(0, "TOKEN")
    val stmt = RuleType(1, "statement")
    val expr = RuleType(2, "expression")

    val tree = astNode(stmt) {
        token("if", token, 1)
        token("(", token)
        node(expr) {
            token("true", token)
        }
        token(")", token)
        node(stmt) {
            node(expr) {
                token("invoke", token, 2)
                token("(", token)
                token(")", token)
            }
        }
    }

    println(tree.accept(AstPrintVisitor))
}