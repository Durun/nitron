package io.github.durun.nitron.core.config

import io.github.durun.nitron.core.ast.normalizing.NormalizingRuleMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path


@Serializable
data class LangConfig(
        val grammarConfig: GrammarConfig,
        val processConfig: ProcessConfig
)

@Serializable
data class GrammarConfig(
        val grammarFiles: List<String>,
        val utilJavaFiles: List<String>,
        val startRule: String
) {
    fun grammarFilePaths(base: Path): List<Path> {
        return grammarFiles.map { base.resolve(it) }
    }

    fun utilJavaFilesPaths(base: Path): List<Path> {
        return utilJavaFiles.map { base.resolve(it) }
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