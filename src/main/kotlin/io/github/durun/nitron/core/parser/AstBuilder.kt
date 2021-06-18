package io.github.durun.nitron.core.parser

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import java.io.BufferedReader

interface AstBuilder {
    val nodeTypes: NodeTypePool
    fun parse(reader: BufferedReader, entryPoint: String? = null): AstNode
}

object AstBuilders