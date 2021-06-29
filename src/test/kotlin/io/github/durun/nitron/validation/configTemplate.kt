package io.github.durun.nitron.validation

import io.github.durun.nitron.core.config.*
import io.github.durun.nitron.core.config.loader.KSerializationConfigLoader
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement


fun main() = TemporaryTest {
    "NitronConfig" {
        val config = NitronConfig(emptyMap())
        val loader = KSerializationConfigLoader(NitronConfig.serializer())
        val json = loader.json.encodeToJsonElement(config)
        println()
        println(json)
        loader.json.decodeFromJsonElement(json)
    }
    "LangConfig" - {
        "ANTLR Grammar" {
            val config = LangConfig(
                parser = AntlrParserConfig(emptyList(), emptyList(), "startRule"),
                processConfig = ProcessConfig(
                    SplitConfig(emptyList()),
                    NormalizeConfig(emptyMap(), emptyMap(), emptyList())
                ),
                extensions = emptyList()
            )
            val loader = KSerializationConfigLoader(NitronConfig.serializer())
            val json = loader.json.encodeToJsonElement(config)
            println()
            println(json)
            loader.json.decodeFromJsonElement(json)
        }
    }
}.execute()