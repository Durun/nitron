package io.github.durun.nitron.core.ast.type

import io.github.durun.nitron.core.parser.AstBuildVisitor
import org.antlr.v4.runtime.Parser

internal fun AstBuildVisitor.nodeTypePoolOf(antlrParser: Parser): NodeTypePool {
    val tokens = antlrParser.tokenTypeMap.entries
            .groupBy { it.value }
            .mapValues { (_, entries) -> entries.map { it.key } }
            .mapValues { (_, synonyms) ->
                synonyms.filterNot { it.contains('\'') }
                        .firstOrNull()
                        ?: synonyms.first()
            }
            .map { (index, name) -> TokenType(index, name) }
    val rules = antlrParser.ruleNames.asList()
            .mapIndexed { index, name -> RuleType(index, name) }
    return NodeTypePool.of(
            tokenTypes = tokens,
            ruleTypes = rules,
            synonymTokenTypes = tokens.filter { it.index < 0 }
    )
}

fun createNodeTypePool(tokenTypes: List<String>, ruleTypes: List<String>): NodeTypePool {
    return NodeTypePool.of(
            tokenTypes = tokenTypes.mapIndexed {index, name -> TokenType(index, name) },
            ruleTypes = ruleTypes.mapIndexed { index, name -> RuleType(index, name) }
    )
}