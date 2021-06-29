package io.github.durun.nitron.validation

import io.github.durun.nitron.core.ast.visitor.AstPrintVisitor
import io.github.durun.nitron.core.config.loader.NitronConfigLoader
import io.github.durun.nitron.core.parser.AstBuilders
import io.github.durun.nitron.core.parser.jdt.jdt
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
    "JDT Parser" {
        val parser = AstBuilders.jdt()
        sources.forEach { src ->
            val time = measureTime {
                parser.parse(src.reader())
                    .also { println(it.accept(AstPrintVisitor)) }
            }
            println("time JDT  : $time")
        }
    }
    "ANTLR" {
        val config = NitronConfigLoader.load(configPath).langConfig["java"]?.parserConfig ?: throw Exception()
        val parser = config.getParser()
        sources.forEach { src ->
            val time = measureTime {
                parser.parse(src.reader())
                    .also { println(it.accept(AstPrintVisitor)) }
            }
            println("time ANTLR: $time")
        }
    }
}.execute()