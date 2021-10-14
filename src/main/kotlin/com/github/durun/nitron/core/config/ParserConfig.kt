package com.github.durun.nitron.core.config

import com.github.durun.nitron.core.MD5
import com.github.durun.nitron.core.ast.type.NodeType
import com.github.durun.nitron.core.ast.type.createNodeTypePool
import com.github.durun.nitron.core.parser.NitronParser
import com.github.durun.nitron.core.parser.NitronParsers
import com.github.durun.nitron.core.parser.antlr.antlr
import com.github.durun.nitron.core.parser.jdt.jdt
import com.github.durun.nitron.core.parser.srcml.srcml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.toPath


@Serializable
sealed class ParserConfig : ConfigWithDir() {
    abstract fun getParser(): NitronParser
    abstract fun checksum(): MD5
}

@Serializable
@SerialName("antlr")
data class AntlrParserConfig(
    private val grammarFiles: List<String>,
    private val utilJavaFiles: List<String>,
    val startRule: String
) : ParserConfig() {
    val grammarFilePaths: List<Path> by lazy {
        grammarFiles.map { dir.toPath().resolve(it) }   // TODO: Zip内のファイルに対応
    }
    val utilJavaFilePaths: List<Path> by lazy {
        utilJavaFiles.map { dir.toPath().resolve(it) }
    }

    override fun getParser(): NitronParser = NitronParsers.antlr(
        grammarName = fileName,
        entryPoint = startRule,
        grammarFiles = grammarFilePaths,
        utilityJavaFiles = utilJavaFilePaths
    )

    override fun checksum(): MD5 {
        val paths = grammarFilePaths + utilJavaFilePaths
        return paths.map { MD5.digest(it.readText()).toString() }
            .sorted()
            .reduce { a, b -> a + b }
            .let { MD5.digest(it) }
    }
}

@Serializable
@SerialName("jdt")
data class JdtParserConfig(
    val version: String
) : ParserConfig() {
    override fun getParser(): NitronParser = NitronParsers.jdt(version)
    override fun checksum(): MD5 = MD5.digest("JDT Parser $version")
}


@Serializable
@SerialName("srcml")
data class SrcmlParserConfig(
    val command: String,
    val language: String,
    //ex.) "number_literal": {"literal": {"type": "number"}} for <literal type="number">1</literal>
    val tokenTypesWithAttr: Map<String, Map<String, Map<String, String>>>,
    val tokenTypes: List<String>,
    val ruleTypesWithAttr: Map<String, Map<String, Map<String, String>>>,
    val ruleTypes: List<String>
) : ParserConfig() {
    override fun getParser(): NitronParser {
        val types = createNodeTypePool(
            language,
            tokenTypes = tokenTypes + tokenTypesWithAttr.keys,
            ruleTypes = ruleTypes + ruleTypesWithAttr.keys
        )
        //ex.) "literal": {{"type": "number"}: "(number_literal)"}
        val nodeTypeMapping: MutableMap<String, MutableMap<Map<String, String>, NodeType>> = mutableMapOf()

        tokenTypesWithAttr.mapKeys { (it, _) -> types.getTokenType(it)!! }.forEach { (type, map) ->
            val (name, attr) = map.entries.first()
            val attrToType = nodeTypeMapping.computeIfAbsent(name) { mutableMapOf() }
            attrToType[attr] = type
        }
        ruleTypesWithAttr.mapKeys { (it, _) -> types.getRuleType(it)!! }.forEach { (type, map) ->
            val (name, attr) = map.entries.first()
            val attrToType = nodeTypeMapping.computeIfAbsent(name) { mutableMapOf() }
            attrToType[attr] = type
        }
        return NitronParsers.srcml(command, language, types, nodeTypeMapping)
    }

    override fun checksum(): MD5 {
        return MD5.digest(
            language +
                    tokenTypesWithAttr + tokenTypes +
                    ruleTypesWithAttr + ruleTypes
        )
    }
}