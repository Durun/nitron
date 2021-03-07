package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.AstSerializers
import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.NitronConfig
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.GenericParser
import kotlinx.serialization.encodeToString
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import java.util.zip.Deflater
import java.util.zip.Inflater

class ParseUtil(
    val git: Git,
    val config: NitronConfig
) {
    private val parsers: MutableMap<String, GenericParser> = mutableMapOf()
    private val visitors: MutableMap<String, AstBuildVisitor> = mutableMapOf()
    private val encoder = AstSerializers.encodeOnlyJson

    fun readFile(objectId: String): String? {
        val loader = git.repository.open(ObjectId.fromString(objectId))
            ?: return null
        return loader.cachedBytes.decodeToString()
    }

    private fun initParser(langConfig: LangConfig): GenericParser {
        return GenericParser.fromFiles(
            langConfig.grammar.grammarFilePaths,
            langConfig.grammar.utilJavaFilePaths
        )
    }

    private fun getOrInitParser(langName: String, langConfig: LangConfig): GenericParser {
        return parsers[langName] ?: initParser(langConfig)
            .also {
                parsers[langName] = it
                val visitor = AstBuildVisitor(it.antlrParser.grammarFileName, it.antlrParser)
                visitors[langName] = visitor
            }

    }

    fun parseText(text: String, langName: String, langConfig: LangConfig): String {
        val parser = getOrInitParser(langName, langConfig)
        val converter = visitors[langName]!!

        val parseTree = parser.parse(text.reader(), langConfig.grammar.startRule)
        val ast: AstNode = parseTree.accept(converter)
        return encoder.encodeToString(ast)
    }


}

fun ByteArray.deflate(): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION, true)

    deflater.setInput(this)
    deflater.finish()

    val buffer = ByteArray(this.size + 1)
    deflater.deflate(buffer)

    return buffer.sliceArray(0..deflater.bytesRead.toInt())
        .also { deflater.end() }
}

fun ByteArray.inflate(): ByteArray {
    val inflater = Inflater(true)

    inflater.setInput(this)

    val buffer = ByteArray(Short.MAX_VALUE.toInt())
    val length = inflater.inflate(buffer)

    return buffer.sliceArray(0 until length)
        .also { inflater.end() }
}