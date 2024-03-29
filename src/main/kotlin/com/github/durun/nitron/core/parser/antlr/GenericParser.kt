package com.github.durun.nitron.core.parser.antlr

import com.github.durun.nitron.util.logger
import org.antlr.v4.Tool
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.tool.ast.GrammarRootAST
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import org.snt.inmemantlr.comp.CunitProvider
import org.snt.inmemantlr.comp.DefaultCompilerOptionsProvider
import org.snt.inmemantlr.comp.FileProvider
import org.snt.inmemantlr.comp.StringCompiler
import org.snt.inmemantlr.exceptions.ParsingException
import org.snt.inmemantlr.listener.DefaultListener
import org.snt.inmemantlr.memobjects.MemorySource
import org.snt.inmemantlr.stream.DefaultStreamProvider
import org.snt.inmemantlr.stream.StreamProvider
import org.snt.inmemantlr.tool.InmemantlrErrorListener
import org.snt.inmemantlr.tool.InmemantlrTool
import java.io.Reader
import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * [GenericParser] parses sourcecode using given ANTLR grammar files.
 */
class GenericParser
private constructor(
    private val antlr: InmemantlrTool,
    utilityJavaContents: Collection<String>,
    private val useCached: Boolean
) {
    companion object {
        private val LOGGER by logger()

        /**
         * Instantiate [GenericParser]
         * @param grammarContents contents of grammar(.g4) files
         * @param utilityJavaContents contents of utility(.java) files
         */
        fun init(
            grammarContents: Collection<String>,
            utilityJavaContents: Collection<String> = emptySet(),
            toolCustomizer: Tool.() -> Unit = {},
            useCached: Boolean = false
        ): GenericParser {
            val tool = InmemantlrTool()
                .apply(toolCustomizer)
                .apply {
                    val sorted: Collection<GrammarRootAST> = sortGrammarByTokenVocab(grammarContents.toSet())
                    // NOTE: Don't change order of sortGrammarByTokenVocab()
                    sorted.forEach {
                        LOGGER.debug { "gast ${it.grammarName}" }
                        createPipeline(it)
                    }
                }
            check(tool.pipelines.isNotEmpty())
            return GenericParser(tool, utilityJavaContents, useCached)
        }

        /**
         * Instantiate [GenericParser] with files
         * @param grammarFiles paths to grammar(.g4) files
         * @param utilityJavaFiles paths to utility(.java) files
         */
        fun fromFiles(
            grammarFiles: Collection<Path>,
            utilityJavaFiles: Collection<Path> = emptySet(),
            toolCustomizer: Tool.() -> Unit = {},
            useCached: Boolean = false
        ): GenericParser =
            init(grammarFiles.map { it.readText() }, utilityJavaFiles.map { it.readText() }, toolCustomizer, useCached)
    }

    private val compilerOptions: DefaultCompilerOptionsProvider = DefaultCompilerOptionsProvider()

    // Non thread-local
    private val masterCompilerObj by lazy {
        LOGGER.debug { "Compiling" }
        val cUnits: Set<CunitProvider> = setOfNotNull(fileProvider.takeIf { it.hasItems() }) + antlr.compilationUnits
        StringCompiler()
            .apply { compile(cUnits, compilerOptions) }
            .allCompiledObjects
    }

    // Thread-local
    private val compiler: ThreadLocal<StringCompiler> by lazy {
        ThreadLocal.withInitial {
            LOGGER.debug { "Loading compiler object" }
            StringCompiler()
                .apply {
                    synchronized(masterCompilerObj) {
                        load(masterCompilerObj)
                    }
                }
        }
    }

    private val streamProvider: StreamProvider = DefaultStreamProvider()
    private val fileProvider: FileProvider = FileProvider().apply {
        utilityJavaContents.forEach { content ->
            val name = extractClassName(content)
            LOGGER.debug { "add utility" }
            addFiles(MemorySource(name, content))
        }
    }
    private val listener = DefaultListener()
    private val activeLexer: String

    init {
        LOGGER.debug { "process" }
        antlr.pipelines.flatMap { it.items }.forEach {
            LOGGER.debug { "${it.name} $it" }
        }
        val (_, lexer) = antlr.process()
        activeLexer = lexer
    }

    /**
     * internal parser
     * @see Parser
     */
    val antlrParser: Parser by lazy {
        antlr.pipelines.asSequence().mapNotNull {
            runCatching {
                loadParser(
                    parserName = it.parserName,
                    lexer = loadLexer(input = "".reader(), lexerName = activeLexer)
                )
            }.getOrNull()
        }.firstOrNull() ?: throw IllegalStateException("No valid parsers")
    }

    /**
     * Parse given sourcecode.
     * If parsing failed, throws [ParsingException].
     * Due to grammar compilation, it takes long time to parse first time.
     * @param input a reader of sourcecode to parse
     * @param entryPoint the rule name to start parsing
     * @return parse tree of input in ANTLR format
     * @see ParserRuleContext
     * @throws ParsingException
     */
    fun parse(input: Reader, entryPoint: String): ParserRuleContext {

        listener.reset()

        val errorListener = InmemantlrErrorListener()
        val parser = loadParser(
            parserName = selectParser(entryPoint),
            lexer = loadLexer(
                input,
                lexerName = activeLexer
            ).apply { addErrorListener(errorListener) }
        ).apply { addErrorListener(errorListener) }

        listener.setParser(parser)

        val rules = parser.ruleNames
        require(rules.contains(entryPoint))

        val data: ParserRuleContext = run {
            val method: Method = parser.javaClass.getDeclaredMethod(entryPoint)
            method(parser) as ParserRuleContext
        }

        val messages = errorListener.log
            .filterKeys { it == InmemantlrErrorListener.Type.SYNTAX_ERROR }
            .values
        if (messages.isNotEmpty()) throw ParsingException(messages.joinToString(""))

        ParseTreeWalker.DEFAULT.walk(listener, data)

        return data
    }

    private fun selectParser(ruleName: String): String {
        val p = antlr.pipelines.find {
            it.g.ruleNames.contains(ruleName)
        } ?: throw IllegalArgumentException("rule not exist: $ruleName")
        return p.parserName
    }

    private fun loadLexer(input: Reader, lexerName: String): Lexer {
        val stream: CharStream = streamProvider.getCharStream(input.readText())
        return compiler.get().instanciateLexer(stream, lexerName, useCached)
    }

    private fun loadParser(lexer: Lexer, parserName: String): Parser {
        val tokens: CommonTokenStream = CommonTokenStream(lexer)
            .apply { fill() }
        return (compiler.get().instanciateParser(tokens, parserName)
            ?: throw IllegalStateException("failed to instantiate parser"))
            .apply {
                removeErrorListeners()
                interpreter.predictionMode = PredictionMode.LL_EXACT_AMBIG_DETECTION
                buildParseTree = true
                tokenStream = tokens
            }
    }
}

private fun extractClassName(src: String): String {
    val version = JavaCore.VERSION_17
    val parser = ASTParser.newParser(AST.JLS17)
        .apply {
            @Suppress("UNCHECKED_CAST")
            val defaultOptions = DefaultCodeFormatterConstants.getEclipseDefaultSettings() as Map<String, String>
            val options = defaultOptions + mapOf(
                JavaCore.COMPILER_COMPLIANCE to version,
                JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM to version,
                JavaCore.COMPILER_SOURCE to version,
                JavaCore.COMPILER_DOC_COMMENT_SUPPORT to JavaCore.DISABLED
            )
            setCompilerOptions(options)
            setEnvironment(null, null, null, true)
        }
    val unit = try {
        parser.setSource(src.toCharArray())
        val result: CompilationUnit = parser.createAST(null) as CompilationUnit  // can be null
        result
    } catch (e: NullPointerException) {
        throw Exception("Failed to parse utility java file", e)
    } catch (e: Exception) {
        throw Exception("Internal error: ${e.message}", e)
    }
    val cls = (unit.types().firstOrNull() as? AbstractTypeDeclaration) ?: throw Exception("Utility java file has no classes.")
    return cls.name.toString()
}
