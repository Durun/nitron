package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.AstSerializers
import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.NitronConfig
import io.github.durun.nitron.core.parser.AstBuildVisitor
import io.github.durun.nitron.core.parser.GenericParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId

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