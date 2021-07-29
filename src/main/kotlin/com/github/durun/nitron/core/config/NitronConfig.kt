package com.github.durun.nitron.core.config

import com.github.durun.nitron.core.ast.path.AstPath
import com.github.durun.nitron.core.ast.processors.AstNormalizer
import com.github.durun.nitron.core.ast.processors.AstSplitter
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.config.loader.LangConfigLoader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path

abstract class ConfigWithDir {
    internal lateinit var path: Path

    val dir: Path
        get() = path.toAbsolutePath().parent

    val fileName: String
        get() = path.fileName.toString()

    val filePath: Path
        get() = path
}

internal fun <C : ConfigWithDir> C.setPath(filePath: Path): C {
    this.path = filePath
    return this
}

@Serializable
data class NitronConfig(
        private val langConfigFiles: Map<String, String>
) : ConfigWithDir() {
    val langConfig: Map<String, LangConfig> by lazy {
        langConfigFiles.mapValues {
            val file = dir.resolve(it.value)
            LangConfigLoader.load(file)
        }
    }
}

@Serializable
data class LangConfig(
    @SerialName("parserConfig") private val parser: ParserConfig,
    val processConfig: ProcessConfig,
    val extensions: List<String>
) : ConfigWithDir() {
    val parserConfig: ParserConfig by lazy { parser.setPath(path) }
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