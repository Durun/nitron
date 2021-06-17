package io.github.durun.nitron.core.parser

import io.github.durun.nitron.core.ast.node.AstNode
import java.io.BufferedReader

interface AstBuilder {
    fun parse(reader: BufferedReader, entryPoint: String? = null): AstNode
}