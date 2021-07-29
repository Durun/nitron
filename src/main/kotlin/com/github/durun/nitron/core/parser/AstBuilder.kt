package com.github.durun.nitron.core.parser

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.type.NodeTypePool
import java.io.Reader

interface AstBuilder {
    val nodeTypes: NodeTypePool
    fun parse(reader: Reader): AstNode
}

object AstBuilders