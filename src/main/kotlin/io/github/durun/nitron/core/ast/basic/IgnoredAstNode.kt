package io.github.durun.nitron.core.ast.basic

class IgnoredAstNode : AstNode {
    override val range: TextRange? = null
    override val children: List<AstNode>? = null
    override fun getText(): String? = ""
    override fun pickByRules(rules: Collection<String>): List<AstNode> = TODO()
    override fun pickRecursiveByRules(rules: Collection<String>): List<AstNode> = TODO()
    override fun mapChildren(map: (AstNode) -> AstNode): AstNode = TODO()
}