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
    "read antlr config via url" {
        val url = ClassLoader.getSystemResource("nitronConfig/nitron.json")
        val config = NitronConfigLoader.load(url)
        println("loaded ${config.fileUri}")
        val langConfig = config.langConfig["golang"]!!
        println("loaded ${langConfig.fileUri}")
        val parser = langConfig.parserConfig.getParser()
        println("loaded parser: $parser")
    }
})

