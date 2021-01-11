package io.github.durun.nitron.core.parser

import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.snt.inmemantlr.GenericParser
import org.snt.inmemantlr.listener.DefaultListener
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

class CommonParser
private constructor(
		private val gParser: GenericParser,
		utilityJavaFiles: Collection<Path> = emptySet()
) {
	@ExperimentalPathApi
    constructor(
			grammarFiles: Collection<Path>,
			utilityJavaFiles: Collection<Path> = emptySet()
	) : this(
			GenericParser(
					{ it },
					*grammarFiles.map { it.readText() }.toTypedArray()
			),
			utilityJavaFiles
	)

	val antlrParser: Parser by lazy {
		runCatching { gParser.parse("") }
		ParserListener.getParser() ?: throw IllegalStateException("couldn't get parser")
	}

	init {
		gParser.setListener(ParserListener)
		utilityJavaFiles.forEach {
			gParser.addUtilityJavaFiles(it.toFile())
		}
		gParser.compile()
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

