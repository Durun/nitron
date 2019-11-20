package io.github.durun.nitron.core.ast.normalizing

import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.basic.TextRange

class IgnoredAstNode : AstNode {
    override val range: TextRange? = null
    override val children: List<AstNode>? = null
    override fun getText(): String? = ""
}