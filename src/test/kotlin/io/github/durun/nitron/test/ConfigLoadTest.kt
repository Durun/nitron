package io.github.durun.nitron.test

import io.github.durun.nitron.core.config.loader.LangConfigLoader
import io.kotest.core.spec.style.FreeSpec
import java.nio.file.Paths

class ConfigLoadTest : FreeSpec({
    "LangConfig" - {
        "KSerialize" {
            val file = Paths.get("config/lang/kotlin.json")
            val config = LangConfigLoader.load(file)
            println(config)
        }
    }
})