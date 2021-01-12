package io.github.durun.nitron.core.parser

import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.snt.inmemantlr.GenericParser
import org.snt.inmemantlr.listener.DefaultListener
import java.nio.file.Path

class CommonParser constructor(
		grammarFiles: Collection<Path>,
		utilityJavaFiles: Collection<Path> = emptySet()
) {
	private val gParser: GenericParser by lazy {
		GenericParser(
				*grammarFiles.map { it.toFile() }.toTypedArray()
		).apply {
			setListener(ParserListener)
			utilityJavaFiles.forEach {
				addUtilityJavaFiles(it.toFile())
			}
			compile()
		}
	}

	val antlrParser: Parser by lazy {
		runCatching { gParser.parse("") }
		ParserListener.getParser() ?: throw IllegalStateException("couldn't get parser")
	}

	fun parse(input: String, startRuleName: String?): ParserRuleContext {
		return gParser.parse(
				input,
				startRuleName,
				GenericParser.CaseSensitiveType.NONE
		)
	}

	fun parse(input: Path, startRuleName: String?): Pair<ParserRuleContext, Parser> {
		val tree = gParser.parse(
				input.toFile(),
				startRuleName,
				GenericParser.CaseSensitiveType.NONE
		)
		return Pair(tree, antlrParser)
	}

	private object ParserListener : DefaultListener() {
		fun getParser(): Parser? = this.parser
	}
}

