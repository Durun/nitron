package io.github.durun.nitron.binding.cpanalyzer

import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.AstVisitor
import io.github.durun.nitron.core.ast.basic.AstBuildVisitor
import io.github.durun.nitron.core.ast.normalizing.IgnoredAstNode
import io.github.durun.nitron.core.ast.normalizing.NormalizePrintVisitor
import io.github.durun.nitron.core.ast.normalizing.NormalizingRuleMap
import io.github.durun.nitron.core.ast.visitor.AstIgnoreVisitor
import io.github.durun.nitron.core.ast.visitor.AstSplitVisitor
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.parser.CommonParser

class CodeProcessor(
        private val config: LangConfig
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

    fun process(input: String): List<Pair<AstNode, String>> {
        val (tree, antlrParser) = parser.parse(input, startRule)
        val ast = tree.accept(AstBuildVisitor(antlrParser))
        val statements = ast
                .accept(splitVisitor)
        return statements
                .map { it.accept(ignoreVisitor) }
                .filterNot { it is IgnoredAstNode }
                .map {
                    val normalizeVisitor = NormalizePrintVisitor(nonNumberedRuleMap, numberedRuleMap)
                    Pair(it, it.accept(normalizeVisitor))
                }
    }
}