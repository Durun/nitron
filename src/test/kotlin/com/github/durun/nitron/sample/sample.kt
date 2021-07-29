package com.github.durun.nitron.sample

import com.github.durun.nitron.core.ast.path.AstPath
import com.github.durun.nitron.core.ast.processors.AstNormalizer
import com.github.durun.nitron.core.ast.processors.AstSplitter
import com.github.durun.nitron.core.config.loader.NitronConfigLoader
import com.github.durun.nitron.core.parser.AstBuilders
import com.github.durun.nitron.core.parser.antlr.antlr
import com.github.durun.nitron.core.parser.jdt.jdt
import java.io.StringReader
import java.nio.file.Path


/**
 * 設定ファイルから読み込んだ Parser でパースする
 */
fun sample_ParserFromConfig(src: String) {
    // 全体の設定 NitronConfig を読み込みます
    val config = NitronConfigLoader.load(Path.of("config/nitron.json"))

    // 言語毎の設定 LangConfig を取り出します
    // 言語一覧は config.nitron.json に定義されています
    // "java" は config/lang/java.json にあります
    val javaConfig = config.langConfig["java"]!!

    val parser = javaConfig.parserConfig.getParser()
    val ast = parser.parse(StringReader(src))
    println(ast)
}

/**
 * 設定ファイルから読み込んだ AstNormalizer で正規化する
 */
fun sample_NormalizeFromConfig(src: String) {
    val config = NitronConfigLoader.load(Path.of("config/nitron.json"))
    val javaConfig = config.langConfig["java"]!!
    val parser = javaConfig.parserConfig.getParser()
    val ast = parser.parse(src.reader())

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    val types = parser.nodeTypes
    // AstNormalizer を初期化します
    // NormalizeConfig に正規化用の設定が格納されています
    val normConfig = javaConfig.processConfig.normalizeConfig
    val normalizer = normConfig.initNormalizer(types)

    // 正規化を実行します
    val normalizedAst = normalizer.process(ast)
    // 除去規則により ASTの根 が除去された場合、nullが返ります
    println(normalizedAst?.getText())
}

/**
 * 設定ファイルから読み込んだ AstSplitter で分割する
 */
fun sample_SplitFromConfig(src: String) {
    val config = NitronConfigLoader.load(Path.of("config/nitron.json"))
    val javaConfig = config.langConfig["java"]!!
    val parser = javaConfig.parserConfig.getParser()
    val ast = parser.parse(src.reader())

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    val types = parser.nodeTypes
    // AstSplitter を初期化します
    // SplitConfig に正規化用の設定が格納されています
    val splitConfig = javaConfig.processConfig.splitConfig
    val splitter = splitConfig.initSplitter(types)

    // 分割を実行します
    val statements = splitter.process(ast)
    println(statements)
}

/**
 * 直接 JDT Parser を設定してパースする
 */
fun sample_JDTParser(src: String) {
    // パーサはAstBuilderインターフェースを持ちます
    val parser = AstBuilders.jdt()
    val ast = parser.parse(src.reader())
    println(ast)
}

/**
 * 直接 ANTLR を設定してパースする
 */
fun sample_ANTLRParser(src: String) {
    // 各種設定を入れてANTLRパーサを生成します
    val parser = AstBuilders.antlr(
        grammarName = "java",           // 名前
        entryPoint = "compilationUnit", // 翻訳単位の非終端記号名
        grammarFiles = listOf(          // 文法ファイル
            Path.of("config/grammars/java/java8/Java8Lexer.g4"),
            Path.of("config/grammars/java/java8/Java8Parser.g4")
        ),
        utilityJavaFiles = listOf()     // 必要なjavaファイル (この例では無し)
    )
    val ast = parser.parse(src.reader())
    println(ast)
}

/**
 * 直接 AstNormalizer を設定してASTを正規化する
 */
fun sample_NormalizeAst(src: String) {
    val config = NitronConfigLoader.load(Path.of("config/nitron.json"))
    val javaConfig = config.langConfig["java"]!!
    val parser = javaConfig.parserConfig.getParser()
    val ast = parser.parse(src.reader())

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    val types = parser.nodeTypes

    // AstNormalizer を設定します
    val normalizer = AstNormalizer(
        mapping = mapOf(
            AstPath.of("IntegerLiteral", types) to "N"
        ),
        numberedMapping = mapOf(
            AstPath.of("variableDeclaratorId", types) to "V",
            AstPath.of("//expressionName/Identifier", types) to "V" // XPathで指定する場合、//は省略できません
        ),
        ignoreRules = listOf(
            AstPath.of("annotation", types)
        )
    )

    // 正規化を実行します
    val normalizedAst = normalizer.process(ast)
    // 除去規則により ASTの根 が除去された場合、nullが返ります
    println(normalizedAst?.getText())
}

/**
 * 直接 AstSplitter を設定してASTを分割する
 */
fun sample_SplitAst(src: String) {
    val config = NitronConfigLoader.load(Path.of("config/nitron.json"))
    val javaConfig = config.langConfig["java"]!!
    val parser = javaConfig.parserConfig.getParser()
    val ast = parser.parse(src.reader())

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    val types = parser.nodeTypes

    // AstSplitter を設定します
    val splitter = AstSplitter(
        listOf(
            types.getType("statement")!!
        )
    )

    // 分割を実行します
    val statements = splitter.process(ast)
    println(statements)
}

fun main() {
    val src = """
        public class NitronSample {
            public static void main(String[] args) {
                int x = 1;
                System.out.println("Hello!");
            }
        }
    """.trimIndent()
    sample_ParserFromConfig(src)
    sample_NormalizeFromConfig(src)
    sample_SplitFromConfig(src)
    sample_JDTParser(src)
    sample_ANTLRParser(src)
    sample_NormalizeAst(src)
    sample_SplitAst(src)
}