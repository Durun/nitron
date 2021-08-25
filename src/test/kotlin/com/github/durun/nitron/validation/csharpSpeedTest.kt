package com.github.durun.nitron.validation

import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import java.nio.file.Paths
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val configPath = Paths.get("config/nitron.json")

/**
 * @param args filepath to parse (multiple OK)
 */
@ExperimentalTime
fun main(args: Array<String>) = TemporaryTest {
    val sources = args.map { Paths.get(it).toFile() }
    "srcML" {
        val config = NitronConfigLoader.load(configPath).langConfig["csharp-srcml"]?.parserConfig ?: throw Exception()
        val parser = config.getParser()
        sources.forEach { src ->
            val time = measureTime {
                parser.parse(src.reader())
            }
            println("time srcML: $time")
        }
    }
    "ANTLR" {
        val config = NitronConfigLoader.load(configPath).langConfig["csharp"]?.parserConfig ?: throw Exception()
        val parser = config.getParser()
        sources.forEach { src ->
            val time = measureTime {
                parser.parse(src.reader())
            }
            println("time ANTLR: $time")
        }
    }
}.execute()