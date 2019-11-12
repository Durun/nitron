package io.github.durun.nitron.core.config

import io.github.durun.nitron.core.ast.visitor.normalizing.NormalizingRuleMap
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path

abstract class ConfigWithDir {
    internal lateinit var dir: Path
}

fun <C : ConfigWithDir> C.setDir(dir: Path): C {
    this.dir = dir
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
        private val grammarConfig: GrammarConfig,
        private val processConfig: ProcessConfig,
        val extensions: List<String>
) : ConfigWithDir() {
    val grammar: GrammarConfig
        get() = grammarConfig.setDir(dir)
    val process: ProcessConfig
        get() = processConfig
}

@Serializable
data class GrammarConfig(
        private val grammarFiles: List<String>,
        private val utilJavaFiles: List<String>,
        val startRule: String
) : ConfigWithDir() {
    val grammarFilePaths: List<Path> by lazy {
        grammarFiles.map { dir.resolve(it) }
    }
    val utilJavaFilePaths: List<Path> by lazy {
        utilJavaFiles.map { dir.resolve(it) }
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
)

@Serializable
data class NormalizeConfig(
        @SerialName("nonNumberedRuleMap")
        val nonNumberedRuleMapConfig: List<RuleMapConfig>,
        @SerialName("numberedRuleMap")
        val numberedRuleMapConfig: List<RuleMapConfig>,

        val ignoreRules: List<String>
) {
    private fun toRuleMap(list: List<RuleMapConfig>): NormalizingRuleMap {
        return NormalizingRuleMap(
                ruleMap = list
                        .map {
                            it.fromRules to it.toSymbol
                        }.toMap()
        )
    }

    val nonNumberedRuleMap: NormalizingRuleMap
        get() = toRuleMap(nonNumberedRuleMapConfig)

    val numberedRuleMap: NormalizingRuleMap
        get() = toRuleMap(numberedRuleMapConfig)
}

@Serializable
data class RuleMapConfig(
        val fromRules: List<String>,
        val toSymbol: String
)