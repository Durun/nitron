package io.github.durun.nitron.tester

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.ParserStore
import org.snt.inmemantlr.exceptions.ParsingException
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedReader

@ExperimentalPathApi
class ParserTester(
		grammarFiles: Collection<Path>,
		private val startRuleName: String,
		private val inputFiles: Collection<Path>,
		utilityJavaFiles: Collection<Path> = emptySet()
) {
	private val parser = ParserStore.getOrThrow(grammarFiles, utilityJavaFiles)
	fun getAsts(): List<AstNode> {
		// Pair<Result, Path>
		val results = inputFiles.map { file ->
			runCatching {
				val tree = parser.parse(file.bufferedReader(), startRuleName)
				val parser = parser.antlrParser
				tree.accept(AstBuildVisitor(grammarName = null, parser))
			} to file
		}

		val exceptions = results.map { it.first.exceptionOrNull() to it.second }
				.filter { it.first != null }
		if (exceptions.isNotEmpty()) {
			throw ParsingException(
					exceptions.joinToString("\n") { "\n" + "${it.second.toFile().name}: ${it.first?.message}".prependIndent("\t") },
					exceptions.first().first!!
			)
		}

		return results.mapNotNull { it.first.getOrNull() }
	}
}