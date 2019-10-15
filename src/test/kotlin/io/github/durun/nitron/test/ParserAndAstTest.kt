package io.github.durun.nitron.test

import io.github.durun.nitron.tester.ParserTester
import io.github.durun.nitron.tester.reserializeJson
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors


val baseDir = Paths.get("testdata/grammars").toAbsolutePath()

class ParserAndAstTest: StringSpec({
    "parse calculator" {
        val grammarFiles = listOf(baseDir.resolve("calculator/calculator.g4"))
        val exampleFiles = listOf(baseDir.resolve("calculator/examples/number1.txt"))
        val startRule = "equation"
        val asts = ParserTester(grammarFiles, startRule, exampleFiles).getAsts()
        asts.forEach {
            val jsons = reserializeJson(it)
            jsons.first shouldBe jsons.second
        }
    }

    "parse and json serialize" {
        forall(
                row("kotlin", ".kt", "kotlinFile"),
                row("java", ".java", "compilationUnit"),
                row("java8", ".java", "compilationUnit"),
                row("java9", ".java", "compilationUnit"),
                row("c", ".c", "compilationUnit"),
                row("python3", ".py", "file_input"),
                row("ruby", ".rb", "prog"),
                row("scala", ".txt", "compilationUnit"),
                row("swift3", ".swift", "top_level"),
                row("typescript", ".ts", "program"),
                row("javascript", ".js", "program"),
                row("golang", ".go", "sourceFile"),
                row("csharp", ".cs", "compilation_unit"),
                row("cpp", ".cpp", "translationunit"),
                row("objc", ".m", "translationUnit")

        ) { langDir, suffix, startRule ->
            val grammarDirDepth = when(langDir){
                "objc" -> 2
                else -> 1
            }

            val grammarDir = baseDir.resolve(langDir)
            val exampleDir = grammarDir.resolve("examples")
            val grammarFiles = Files.walk(grammarDir, grammarDirDepth)
                    .filter { it.toFile().isFile }
                    .filter { it.toString().endsWith(".g4")}
                    .collect(Collectors.toList())

            val exampleFiles = Files.walk(exampleDir, 3)
                    .filter { it.toFile().isFile }
                    .filter { it.endsWith(suffix) }
                    .collect(Collectors.toList())

            val parserTester = {
                val result = kotlin.runCatching {
                    ParserTester(grammarFiles, startRule, exampleFiles)
                }
                result.getOrNull() ?: throw Exception("failed to compile ${langDir}")
            }()

            val asts = {
                val result = kotlin.runCatching {
                    parserTester.getAsts()
                }
                result.getOrNull() ?: throw Exception("failed to parse in ${langDir}")
            }()

            asts.forEach {
                val jsons = reserializeJson(it)
                jsons.first shouldBe jsons.second
            }
        }
    }
})