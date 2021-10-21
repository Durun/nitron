package com.github.durun.nitron.core.parser

import com.github.durun.nitron.core.ParsingException
import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.type.NodeTypePool
import java.io.Reader

interface NitronParser {
    val nodeTypes: NodeTypePool

    @Throws(ParsingException::class)
    fun parse(reader: Reader): AstNode
}

object NitronParsers