package com.github.durun.nitron.core.parser.antlr

import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.ParseTree
import org.snt.inmemantlr.utils.Tuple

operator fun <K, T> Tuple<K, T>.component1(): K = this.first
operator fun <K, T> Tuple<K, T>.component2(): T = this.second

val ParseTree.children: List<ParseTree>
    get() = (0 until this.childCount).map(this::getChild)

fun nodeTypePoolOf(grammarName: String?, antlrParser: Parser): NodeTypePool {
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
        grammarName = grammarName ?: antlrParser.grammarFileName,
        tokenTypes = tokens,
        ruleTypes = rules,
        synonymTokenTypes = allTokens.filterNot { tokens.any { other -> it === other } }
    )
}