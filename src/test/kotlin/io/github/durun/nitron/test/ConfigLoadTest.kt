package io.github.durun.nitron.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.kotest.core.spec.style.FreeSpec
import java.nio.file.Paths

class ConfigLoadTest : FreeSpec() {
    init {
        "LangConfig" - {
            "KSerialize" {
                val file = Paths.get("config/lang/kotlin.json")
                val config = LangConfigLoader.load(file)
                println(config)
            }

            "!Jackson" {
                val file = Paths.get("config/lang/kotlin.json")
                val config = jacksonObjectMapper().readValue<LangConfig>(file.toFile())
                println(config)
            }
        }
    }
}