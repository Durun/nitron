package io.github.durun.nitron.validation

import io.github.durun.nitron.tester.ParserTester
import io.kotlintest.matchers.collections.shouldHaveAtLeastSize
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors


fun main() = TemporaryTest {
    "parse and dump" - {
        "calculator" {
            val grammarFiles = listOf(baseDir.resolve("calculator/calculator.g4"))
            val exampleFiles = listOf(baseDir.resolve("calculator/examples/number1.txt"))
            val startRule = "equation"
            val asts = ParserTester(grammarFiles, startRule, exampleFiles).getAsts()
            asts shouldHaveAtLeastSize 1
        }
        "C" {
            testDefault("c", ".c", startRule = "compilationUnit")
        }
        "!C++" {
            testDefault("cpp", ".cpp", startRule = "translationunit")
        }
        "C#" {
            testDefault("csharp", ".cs", startRule = "compilation_unit")
        }
        "!Go" {
            testDefault("golang", ".go", startRule = "sourceFile")
        }
        "Java" {
            testDefault("java/java", ".java", startRule = "compilationUnit")
        }
        "Java8" {
            testDefault("java/java8", ".java", startRule = "compilationUnit")
        }
        "Java9" {
            testDefault("java/java9", ".java", startRule = "compilationUnit")
        }
        "Kotlin" {
            testDefault("kotlin/kotlin-formal", ".kt", startRule = "kotlinFile")
        }
        "Scala" {
            testDefault("scala", ".txt", startRule = "compilationUnit")
        }
        "!TypeScript" {
            testDefaultWithUtilFiles("typescript", ".ts", startRule = "program")
        }
        "!Objective-C" {
            val lang = "objc"
            val suffix = ".m"
            val startRule = "translationUnit"
            val dir = baseDir.resolve(lang)
            testParsing(
                    grammarFiles = collectFiles(dir, ".g4", depth = 2),
                    exampleFiles = collectFiles(dir.resolve("examples"), suffix),
                    startRule = startRule
            )
        }
        "Python3" {
            testDefault("python/python3", ".py", startRule = "file_input")
        }
        "Ruby" {
            testDefault("ruby", ".rb", startRule = "prog")
        }
        "Swift3" {
            testDefaultWithUtilFiles("swift/swift3", ".swift", startRule = "top_level")
        }
        "R" {
            testParsing(
                    grammarFiles = listOf(baseDir.resolve("r/R.g4")),
                    exampleFiles = collectFiles(baseDir.resolve("r/examples"), ".txt"),
                    startRule = "prog"
            )
        }
        "JavaScript" {
            val dir = baseDir.resolve("javascript/javascript")
            testParsing(
                    grammarFiles = collectFiles(dir, ".g4"),
                    utilFiles = collectFiles(dir.resolve("Java"), ".java"),
                    exampleFiles = collectFiles(dir.resolve("examples"), ".js")
                            .filterNot {
                                // current JS grammar sometimes can't parse "for-of" statement
                                val exclude = listOf(
                                        "MapSetAndWeakMapWeakSet.js",
                                        "Generators.js",
                                        "Scoping.js",
                                        "Iterators.js",
                                        "ExtendedLiterals.js",
                                        "Misc.js",
                                        "EnhancedRegularExpression.js"
                                )
                                exclude.contains(it.toFile().name)
                            },
                    startRule = "program"
            )
        }
        "Matlab" {
            testDefault("matlab", ".txt", startRule = "translation_unit")
        }
        "Erlang" {
            testParsing(
                    grammarFiles = collectFiles(baseDir.resolve("erlang"), ".g4"),
                    exampleFiles = listOf(baseDir.resolve("erlang/examples/helloword.erl")),
                    startRule = "forms"
            )
        }
    }
}.execute()

private val baseDir = Paths.get("config/grammars").toAbsolutePath()

private fun collectFiles(dir: Path,
                         suffix: String? = null, exclude: String? = null, depth: Int = 1): List<Path> {
    val allFiles = Files.walk(dir, depth).filter { it.toFile().isFile }
    val matchedFiles = suffix?.let { allFiles.filter { it.toString().endsWith(suffix) } } ?: allFiles
    val resultFiles = exclude?.let { matchedFiles?.filter { !it.toString().contains(exclude) } } ?: matchedFiles
    return resultFiles.collect(Collectors.toList())
}

private fun testDefault(lang: String, suffix: String, startRule: String) {
    val grammarDir = baseDir.resolve(lang)
    val exampleDir = grammarDir.resolve("examples")
    testParsing(
            grammarFiles = collectFiles(grammarDir, suffix = ".g4"),
            exampleFiles = collectFiles(exampleDir, suffix = suffix, depth = 3),
            startRule = startRule
    )
}

private fun testDefaultWithUtilFiles(lang: String, suffix: String, startRule: String) {
    val grammarDir = baseDir.resolve(lang)
    val exampleDir = grammarDir.resolve("examples")
    testParsing(
            grammarFiles = collectFiles(grammarDir, suffix = ".g4"),
            exampleFiles = collectFiles(exampleDir, suffix = suffix, depth = 3),
            startRule = startRule,
            utilFiles = collectFiles(grammarDir, suffix = ".java", exclude = "example", depth = 5)
    )
}

private fun testParsing(
        grammarFiles: List<Path>,
        exampleFiles: List<Path>,
        startRule: String,
        utilFiles: List<Path>? = null
) {
    println("""
        [testJsonDump]
        grammarFiles = ${grammarFiles.joinToString()}
        exampleFiles = ${exampleFiles.joinToString()}
        utilFiles = ${utilFiles?.joinToString()}
        startRule = $startRule

    """.trimIndent())
    // compile
    val parserTester = ParserTester(grammarFiles, startRule, exampleFiles, utilFiles)
    println("compile: OK")
    // parse
    val asts = parserTester.getAsts()
    println("parse: OK")
}
