package io.github.durun.nitron.core.ast.basic

class IgnoredAstNode : AstNode {
    override val range: TextRange? = null
    override val children: List<AstNode>? = null
    override fun getText(): String? = ""
}