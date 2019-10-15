package io.github.durun.nitron.tester

import io.github.durun.nitron.ast.basic.AstBuildVisitor
import io.github.durun.nitron.ast.AstNode
import io.github.durun.nitron.parser.CommonParser
import java.nio.file.Path

class ParserTester(
        val grammarFiles: List<Path>,
        val startRuleName: String,
        val inputFiles: List<Path>
) {
    private val parser = CommonParser(grammarFiles)
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