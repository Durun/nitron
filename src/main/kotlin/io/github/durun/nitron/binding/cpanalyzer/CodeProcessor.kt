package io.github.durun.nitron.binding.cpanalyzer

import io.github.durun.nitron.core.ast.AstNode
import io.github.durun.nitron.core.ast.AstVisitor
import io.github.durun.nitron.core.ast.basic.AstBuildVisitor
import io.github.durun.nitron.core.ast.normalizing.IgnoredAstNode
import io.github.durun.nitron.core.ast.normalizing.NormalizePrintVisitor
import io.github.durun.nitron.core.ast.normalizing.NormalizingRuleMap
import io.github.durun.nitron.core.ast.visitor.AstIgnoreVisitor
import io.github.durun.nitron.core.ast.visitor.AstSplitVisitor
import io.github.durun.nitron.core.config.LangConfigLoader
import io.github.durun.nitron.core.parser.CommonParser
import java.nio.file.Path

class CodeProcessor(configFile: Path) {
    private val parser: CommonParser
    private val startRule: String
    private val splitVisitor: AstVisitor<List<AstNode>>
    private val nonNumberedRuleMap: NormalizingRuleMap
    private val numberedRuleMap: NormalizingRuleMap
    private val ignoreVisitor: AstVisitor<AstNode>

    init {
        val config = LangConfigLoader.load(configFile)
        val baseDir = configFile.toAbsolutePath().parent
        parser = CommonParser(
                grammarFiles = config.grammarConfig.grammarFilePaths(baseDir),
                utilityJavaFiles = config.grammarConfig.utilJavaFilesPaths(baseDir)
        )
        startRule = config.grammarConfig.startRule
        splitVisitor = AstSplitVisitor(config.processConfig.splitConfig.splitRules)
        nonNumberedRuleMap = config.processConfig.normalizeConfig.nonNumberedRuleMap
        numberedRuleMap = config.processConfig.normalizeConfig.numberedRuleMap
        ignoreVisitor = AstIgnoreVisitor(config.processConfig.normalizeConfig.ignoreRules)
        println("Parser compiled: config=${configFile}")   // TODO
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