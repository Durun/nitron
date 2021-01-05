package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.parser.AstBuildVisitor
import org.antlr.v4.runtime.Parser

internal fun AstBuildVisitor.nodeTypePoolOf(antlrParser: Parser): NodeTypePool {
    val allTokens = antlrParser.tokenTypeMap.entries
            .map { (name, index) -> TokenType(index, name) }
    val tokens = allTokens
            .groupBy { it.index }
            .map { (_, candidates) ->
                candidates.filterNot { it.name.contains('\'') }
                        .firstOrNull()
                        ?: candidates.first()
            }
    val rules = antlrParser.ruleNames.asList()
            .mapIndexed { index, name -> RuleType(index, name) }
    return NodeTypePool.of(
            grammarName = antlrParser.grammarFileName,
            tokenTypes = tokens,
            ruleTypes = rules,
            synonymTokenTypes = allTokens.filterNot { tokens.any { other -> it === other } }
    )
}

fun createNodeTypePool(grammarName: String, tokenTypes: List<String>, ruleTypes: List<String>): NodeTypePool {
    return NodeTypePool.of(
            grammarName,
            tokenTypes = tokenTypes.mapIndexed {index, name -> TokenType(index, name) },
            ruleTypes = ruleTypes.mapIndexed { index, name -> RuleType(index, name) }
    )
}