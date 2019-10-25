package io.github.durun.nitron.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.LangConfigLoader
import io.kotlintest.specs.FreeSpec
import java.nio.file.Paths

class ConfigLoadTest : FreeSpec() {
    init {
        "LangConfig" - {
            "KSerialize" {
                val file = Paths.get("testdata/kotlin.json")
                val config = LangConfigLoader.load(file)
                println(config)
            }

            "!Jackson" {
                val file = Paths.get("testdata/kotlin.json")
                val config = jacksonObjectMapper().readValue<LangConfig>(file.toFile())
                println(config)
            }
        }
    }
}