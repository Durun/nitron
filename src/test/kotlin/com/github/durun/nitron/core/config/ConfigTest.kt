package com.github.durun.nitron.core.config

import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.kotest.core.spec.style.FreeSpec

class JdtParserTest : FreeSpec({
    "read from uri" {
        val uri = ClassLoader.getSystemResource("nitronConfig/nitron.json").toURI()
        val config = NitronConfigLoader.load(uri)
        println("loaded ${config.fileUri}")
        val langConfig = config.langConfig["java-jdt"]
        println("loaded ${langConfig?.fileUri}")
    }
})

