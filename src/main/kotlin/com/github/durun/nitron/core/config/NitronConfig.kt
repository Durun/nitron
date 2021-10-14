package com.github.durun.nitron.core.config

import com.github.durun.nitron.core.ast.path.AstPath
import com.github.durun.nitron.core.ast.processors.AstNormalizer
import com.github.durun.nitron.core.ast.processors.AstSplitter
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.config.loader.LangConfigLoader
import com.github.durun.nitron.util.resolveInJar
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI

abstract class ConfigWithDir {
    internal lateinit var uri: URI

    val dir: URI
        get() = uri.resolveInJar(".").normalize()

    val fileName: String
        get() = uri.toURL().file

    val fileUri: URI
        get() = uri
}

@Serializable
data class NitronConfig(
        private val langConfigFiles: Map<String, String>
) : ConfigWithDir() {
    val langConfig: Map<String, LangConfig> by lazy {
        langConfigFiles.mapValues {
            val filePath = dir.resolveInJar(it.value)
            LangConfigLoader.load(filePath)
        }
    }
}

@Serializable
data class LangConfig(
    @SerialName("parserConfig") private val parser: ParserConfig,
    val processConfig: ProcessConfig,
    val extensions: List<String>
) : ConfigWithDir() {
    val parserConfig: ParserConfig by lazy {
        parser.uri = uri
        parser
    }
}

@Serializable
data class ProcessConfig(
        val splitConfig: SplitConfig,
        val normalizeConfig: NormalizeConfig
)

@Serializable
data class SplitConfig(
    val splitRules: List<String>
) {
    fun initSplitter(types: NodeTypePool): AstSplitter {
        return AstSplitter(splitRules.map {
            types.getType(it)
                ?: throw NoSuchElementException("Not found $it in $types")
        })
    }
}

@Serializable
data class NormalizeConfig(
    val mapping: Map<String, String>,
    val indexedMapping: Map<String, String>,
    val ignoreRules: List<String>
) {
    fun initNormalizer(types: NodeTypePool): AstNormalizer {
        return AstNormalizer(
            mapping = mapping.entries.associate { (path, symbol) ->
                AstPath.of(path, types) to symbol
            },
            numberedMapping = indexedMapping.entries.associate { (path, symbol) ->
                AstPath.of(path, types) to symbol
            },
            ignoreRules = ignoreRules.map {
                AstPath.of(it, types)
            }
        )
    }
}