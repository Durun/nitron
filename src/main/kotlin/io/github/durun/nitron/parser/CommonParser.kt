package io.github.durun.nitron.parser

import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.snt.inmemantlr.GenericParser
import org.snt.inmemantlr.listener.DefaultListener
import java.io.File
import java.nio.file.Path

class CommonParser
private constructor (
        val gParser: GenericParser
) {
    private val pListener = ParserListener()

    constructor(grammarFiles: List<Path>): this(
                    grammarFiles.map{ it.toFile() }.toTypedArray()
    )
    constructor(grammarFiles: Array<File>): this(
            GenericParser(* grammarFiles)
    )
    init {
        gParser.setListener(pListener)
        gParser.compile()
    }

    fun parse(input: String, startRuleName: String): Pair<ParserRuleContext, Parser> {
        val tree = gParser.parse(
                input,
                startRuleName,
                GenericParser.CaseSensitiveType.NONE
        )
        val parser = pListener.getParser() ?: throw IllegalStateException("couldn't get parser")
        return Pair(tree, parser)
    }
    fun parse(input: Path, startRuleName: String): Pair<ParserRuleContext, Parser> {
        val tree = gParser.parse(
                input.toFile(),
                startRuleName,
                GenericParser.CaseSensitiveType.NONE
        )
        val parser = pListener.getParser() ?: throw IllegalStateException("couldn't get parser")
        return Pair(tree, parser)
    }
}

private class ParserListener(): DefaultListener() {
    fun getParser() = this.parser ?: null
}