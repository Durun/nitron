package io.github.durun.nitron.tester

import io.github.durun.nitron.core.ast.basic.AstNode
import io.github.durun.nitron.core.ast.AstBuildVisitor
import io.github.durun.nitron.core.parser.CommonParser
import java.nio.file.Path

class ParserTester(
        val grammarFiles: List<Path>,
        val startRuleName: String,
        val inputFiles: List<Path>,
        val utilityJavaFiles: List<Path>? = null
) {
    private val parser = CommonParser(grammarFiles, utilityJavaFiles)
    fun getAsts(): List<AstNode> {
        val ast = inputFiles.mapNotNull {
            val result = parser.parse(it, startRuleName)
            val tree = result.first
            val parser = result.second
            tree.accept<AstNode?>(AstBuildVisitor(parser))
        }
        return ast
    }
}