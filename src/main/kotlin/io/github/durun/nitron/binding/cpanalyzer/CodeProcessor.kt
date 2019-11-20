package io.github.durun.nitron.binding.cpanalyzer

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.AstVisitor
import io.github.durun.nitron.core.ast.basic.AstBuildVisitor
import io.github.durun.nitron.core.ast.node.IgnoredAstNode
import io.github.durun.nitron.core.ast.normalizing.NormalizingRuleMap
import io.github.durun.nitron.core.ast.visitor.AstIgnoreVisitor
import io.github.durun.nitron.core.ast.visitor.AstNormalizeVisitor
import io.github.durun.nitron.core.ast.visitor.AstSplitVisitor
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.parser.CommonParser

class CodeProcessor(
        config: LangConfig
) {
    private val parser: CommonParser
    private val startRule: String
    private val splitVisitor: AstVisitor<List<AstNode>>
    private val nonNumberedRuleMap: NormalizingRuleMap
    private val numberedRuleMap: NormalizingRuleMap
    private val ignoreVisitor: AstVisitor<AstNode>

    init {
        parser = CommonParser(
                grammarFiles = config.grammar.grammarFilePaths,
                utilityJavaFiles = config.grammar.utilJavaFilePaths
        )
        startRule = config.grammar.startRule
        splitVisitor = AstSplitVisitor(config.process.splitConfig.splitRules)
        nonNumberedRuleMap = config.process.normalizeConfig.nonNumberedRuleMap
        numberedRuleMap = config.process.normalizeConfig.numberedRuleMap
        ignoreVisitor = AstIgnoreVisitor(config.process.normalizeConfig.ignoreRules)
        println("Parser compiled: config=${config.dir}")   // TODO
    }

    fun parse(input: String): AstNode {
        val (tree, antlrParser) = parser.parse(input, startRule)
        return tree.accept(AstBuildVisitor(antlrParser))
    }

    fun split(input: AstNode): List<AstNode> {
        return input.accept(splitVisitor)
    }

    fun split(input: String): List<AstNode> {
        val ast = parse(input)
        return split(ast)
    }

    private fun dropIgnore(input: AstNode): AstNode? {
        return input
                .accept(ignoreVisitor)
                .takeUnless { it is IgnoredAstNode }
    }

    private fun normalize(input: AstNode): AstNode {
        val visitor = AstNormalizeVisitor(nonNumberedRuleMap, numberedRuleMap)
        return input.accept(visitor)
    }

    fun proceess(input: AstNode): AstNode? {
        return dropIgnore(input)
                ?.let { normalize(it) }
    }

    fun proceess(input: List<AstNode>): List<AstNode> {
        return input.mapNotNull { proceess(it) }
    }

    fun proceessWithOriginal(input: List<AstNode>): List<Pair<AstNode, AstNode?>> {
        return input.map {
            it to proceess(it)
        }
    }
}