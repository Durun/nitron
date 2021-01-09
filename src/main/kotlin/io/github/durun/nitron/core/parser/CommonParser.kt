package io.github.durun.nitron.core.parser

import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.snt.inmemantlr.GenericParser
import org.snt.inmemantlr.listener.DefaultListener
import java.io.File
import java.nio.file.Path

class CommonParser
private constructor(
        val gParser: GenericParser,
        utilityJavaFiles: Array<File>? = null
) {
    private val pListener = ParserListener()

    constructor(
            grammarFiles: List<Path>,
            utilityJavaFiles: List<Path>? = null
    ) : this(
            grammarFiles.map { it.toFile() }.toTypedArray(),
            utilityJavaFiles?.map { it.toFile() }?.toTypedArray()
    )

    constructor(
            grammarFiles: Array<File>,
            utilityJavaFiles: Array<File>? = null
    ) : this(
            GenericParser(
                    { it },
                    false,
                    *grammarFiles.map { it.readText() }.toTypedArray()
            ),
            utilityJavaFiles
    )

    init {
        gParser.setListener(pListener)
        if (!utilityJavaFiles.isNullOrEmpty()) {
            gParser.addUtilityJavaFiles(*utilityJavaFiles)
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
        val parser = pListener.getParser() ?: throw IllegalStateException("couldn't get parser")
        return Pair(tree, parser)
    }

    fun getAntlrParser(): Parser {
        return pListener.getParser()
                ?: gParser.let {
                    kotlin.runCatching { it.parse("") }
                    pListener.getParser() ?: throw IllegalStateException("couldn't get parser")
                }
    }
}

private class ParserListener : DefaultListener() {
    fun getParser() = this.parser ?: null
}