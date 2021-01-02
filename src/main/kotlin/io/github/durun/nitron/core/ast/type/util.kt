package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.parser.AstBuildVisitor
import org.antlr.v4.runtime.Parser

internal fun AstBuildVisitor.nodeTypePoolOf(antlrParser: Parser): NodeTypePool {
    return NodeTypePool(antlrParser.tokenTypeMap, antlrParser.ruleNames.asList())
}