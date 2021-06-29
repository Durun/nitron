package io.github.durun.nitron.core.parser.jdt

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstRuleNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.RuleType
import io.github.durun.nitron.core.ast.type.TokenType
import io.github.durun.nitron.core.ast.visitor.AstVisitor
import io.github.durun.nitron.core.ast.visitor.flatten
import io.github.durun.nitron.core.parser.AstBuilder
import io.github.durun.nitron.core.parser.AstBuilders
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.compiler.ITerminalSymbols
import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import java.io.Reader


@Suppress("UNUSED")
fun AstBuilders.jdt(): AstBuilder = JdtAstBuilder()

private class JdtAstBuilder(version: String = JavaCore.VERSION_16) : AstBuilder {
    override val nodeTypes: NodeTypePool = Companion.nodeTypes

    private val parser = ASTParser.newParser(AST.JLS16)
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

    override fun parse(reader: Reader): AstNode {
        val source = reader.readText().replace(Regex("\r\n|\r|\n"), "\n")
        parser.setSource(source.toCharArray())
        val root = parser.createAST(null)
        val visitor = BuildVisitor(source)
        root.accept(visitor)
        return visitor.result!!
            .accept(AlignLineVisitor())
    }

    private class BuildVisitor(
        val source: String
    ) : ASTVisitor() {
        var result: AstNode? = null
            private set
        private val stack: MutableList<AstNode> = mutableListOf()
        private lateinit var getLineNumber: (Int) -> Int

        override fun preVisit(node: ASTNode) {
            if (node is CompilationUnit) getLineNumber = { node.getLineNumber(it) - 1 } // Line number is 0-origin
            val newNode =
                when (val type = nodeTypes.getRuleType(node.nodeType) ?: nodeTypes.getTokenType(node.nodeType)) {
                    is RuleType -> BasicAstRuleNode(type, mutableListOf())
                    is TokenType -> AstTerminalNode(node.toString(), type, getLineNumber(node.startPosition))
                    else -> throw Exception()
                }
            val parent = stack.lastOrNull()
            if (parent is BasicAstRuleNode) {
                parent.children += newNode
            }
            stack += newNode
        }

        private fun token(text: String, line: Int) = AstTerminalNode(text, TOKEN, line)
        override fun postVisit(node: ASTNode) {
            val parent = stack.lastOrNull()
            if (parent is BasicAstRuleNode && parent.toString() != node.toString()) {
                // 構文木に不足しているトークンを補う
                val trees = parent.children         // 構文木(トークンが不足している)
                val nodeString = source.slice(node.startPosition until node.startPosition + node.length)
                val tokens = lex(nodeString)        // 完全なトークン列
                val baseLine = detectLineNumber(trees, tokens)
                var k = 0
                var remainText = ""
                tokens.forEach { (relLine, token) ->
                    if (trees.lastIndex < k) {
                        trees.add(token(token, baseLine?.plus(relLine) ?: DEFAULT_LINE_NO))
                        k++
                        return@forEach
                    }
                    if (remainText.isEmpty()) remainText = trees[k].accept(CatVisitor)
                    if (remainText.startsWith(token)) {
                        remainText = remainText.drop(token.length)
                        if (remainText.isEmpty()) k++
                    } else {
                        trees.add(k, token(token, baseLine?.plus(relLine) ?: DEFAULT_LINE_NO))
                        k++
                    }
                }
            }

            result = stack.removeLast()
        }

        private fun lex(text: String): List<Pair<Int, String>> {
            val scanner = ToolFactory.createScanner(false, false, true, JavaCore.VERSION_16)
            scanner.source = text.replace(Regex("\r\n|\r|\n"), "\n").toCharArray()
            val list: MutableList<Pair<Int, String>> = mutableListOf()
            var tokenType = scanner.nextToken
            while (tokenType != ITerminalSymbols.TokenNameEOF) {
                val start = scanner.currentTokenStartPosition
                val end = scanner.currentTokenEndPosition
                val line = scanner.getLineNumber(start) - 1 // Line number is 0-origin
                val token = scanner.source.sliceArray(start..end).joinToString("")
                list.add(line to token)
                tokenType = scanner.nextToken
            }
            return list
        }

        /**
         * @param trees 絶対行数の情報を持つ構文木
         * @param tokens (相対行数, トークン) のリスト
         * @return [tokens] の相対行数=0にあたる絶対行数を返す。(0-origin)
         */
        private fun detectLineNumber(trees: List<AstNode>, tokens: List<Pair<Int, String>>): Int? {
            val node = trees.flatMap { it.flatten() }
                .filterIsInstance<AstTerminalNode>().firstOrNull()
                ?: return null
            val matched = tokens.firstOrNull { (_, token) -> token == node.token }
                ?: return null
            val absLine = node.line
            val relLine = matched.first
            return absLine - relLine
        }
    }

    companion object {
        private val DEFAULT_LINE_NO = 0
        val nodeTypes: NodeTypePool = NodeTypePool.of(
            "java",
            tokenTypes = mapOf(
                -1 to "TOKEN",
                ASTNode.BOOLEAN_LITERAL to "BOOLEAN_LITERAL",
                ASTNode.CHARACTER_LITERAL to "CHARACTER_LITERAL",
                ASTNode.NULL_LITERAL to "NULL_LITERAL",
                ASTNode.NUMBER_LITERAL to "NUMBER_LITERAL",
                ASTNode.PRIMITIVE_TYPE to "PRIMITIVE_TYPE",
                ASTNode.SIMPLE_NAME to "SIMPLE_NAME",
                ASTNode.SIMPLE_TYPE to "SIMPLE_TYPE",
                ASTNode.STRING_LITERAL to "STRING_LITERAL",
                ASTNode.TYPE_LITERAL to "TYPE_LITERAL",
                ASTNode.LINE_COMMENT to "LINE_COMMENT",
                ASTNode.BLOCK_COMMENT to "BLOCK_COMMENT",
                ASTNode.MODIFIER to "MODIFIER",
            ),
            ruleTypes = mapOf(
                ASTNode.ANONYMOUS_CLASS_DECLARATION to "ANONYMOUS_CLASS_DECLARATION",
                ASTNode.ARRAY_ACCESS to "ARRAY_ACCESS",
                ASTNode.ARRAY_CREATION to "ARRAY_CREATION",
                ASTNode.ARRAY_INITIALIZER to "ARRAY_INITIALIZER",
                ASTNode.ARRAY_TYPE to "ARRAY_TYPE",
                ASTNode.ASSERT_STATEMENT to "ASSERT_STATEMENT",
                ASTNode.ASSIGNMENT to "ASSIGNMENT",
                ASTNode.BLOCK to "BLOCK",
                ASTNode.BREAK_STATEMENT to "BREAK_STATEMENT",
                ASTNode.CAST_EXPRESSION to "CAST_EXPRESSION",
                ASTNode.CATCH_CLAUSE to "CATCH_CLAUSE",
                ASTNode.CLASS_INSTANCE_CREATION to "CLASS_INSTANCE_CREATION",
                ASTNode.COMPILATION_UNIT to "COMPILATION_UNIT",
                ASTNode.CONDITIONAL_EXPRESSION to "CONDITIONAL_EXPRESSION",
                ASTNode.CONSTRUCTOR_INVOCATION to "CONSTRUCTOR_INVOCATION",
                ASTNode.CONTINUE_STATEMENT to "CONTINUE_STATEMENT",
                ASTNode.DO_STATEMENT to "DO_STATEMENT",
                ASTNode.EMPTY_STATEMENT to "EMPTY_STATEMENT",
                ASTNode.EXPRESSION_STATEMENT to "EXPRESSION_STATEMENT",
                ASTNode.FIELD_ACCESS to "FIELD_ACCESS",
                ASTNode.FIELD_DECLARATION to "FIELD_DECLARATION",
                ASTNode.FOR_STATEMENT to "FOR_STATEMENT",
                ASTNode.IF_STATEMENT to "IF_STATEMENT",
                ASTNode.IMPORT_DECLARATION to "IMPORT_DECLARATION",
                ASTNode.INFIX_EXPRESSION to "INFIX_EXPRESSION",
                ASTNode.INITIALIZER to "INITIALIZER",
                ASTNode.JAVADOC to "JAVADOC",
                ASTNode.LABELED_STATEMENT to "LABELED_STATEMENT",
                ASTNode.METHOD_DECLARATION to "METHOD_DECLARATION",
                ASTNode.METHOD_INVOCATION to "METHOD_INVOCATION",
                ASTNode.PACKAGE_DECLARATION to "PACKAGE_DECLARATION",
                ASTNode.PARENTHESIZED_EXPRESSION to "PARENTHESIZED_EXPRESSION",
                ASTNode.POSTFIX_EXPRESSION to "POSTFIX_EXPRESSION",
                ASTNode.PREFIX_EXPRESSION to "PREFIX_EXPRESSION",
                ASTNode.QUALIFIED_NAME to "QUALIFIED_NAME",
                ASTNode.RETURN_STATEMENT to "RETURN_STATEMENT",
                ASTNode.SINGLE_VARIABLE_DECLARATION to "SINGLE_VARIABLE_DECLARATION",
                ASTNode.SUPER_CONSTRUCTOR_INVOCATION to "SUPER_CONSTRUCTOR_INVOCATION",
                ASTNode.SUPER_FIELD_ACCESS to "SUPER_FIELD_ACCESS",
                ASTNode.SUPER_METHOD_INVOCATION to "SUPER_METHOD_INVOCATION",
                ASTNode.SWITCH_CASE to "SWITCH_CASE",
                ASTNode.SWITCH_STATEMENT to "SWITCH_STATEMENT",
                ASTNode.SYNCHRONIZED_STATEMENT to "SYNCHRONIZED_STATEMENT",
                ASTNode.THIS_EXPRESSION to "THIS_EXPRESSION",
                ASTNode.THROW_STATEMENT to "THROW_STATEMENT",
                ASTNode.TRY_STATEMENT to "TRY_STATEMENT",
                ASTNode.TYPE_DECLARATION to "TYPE_DECLARATION",
                ASTNode.TYPE_DECLARATION_STATEMENT to "TYPE_DECLARATION_STATEMENT",
                ASTNode.VARIABLE_DECLARATION_EXPRESSION to "VARIABLE_DECLARATION_EXPRESSION",
                ASTNode.VARIABLE_DECLARATION_FRAGMENT to "VARIABLE_DECLARATION_FRAGMENT",
                ASTNode.VARIABLE_DECLARATION_STATEMENT to "VARIABLE_DECLARATION_STATEMENT",
                ASTNode.WHILE_STATEMENT to "WHILE_STATEMENT",
                ASTNode.INSTANCEOF_EXPRESSION to "INSTANCEOF_EXPRESSION",
                ASTNode.TAG_ELEMENT to "TAG_ELEMENT",
                ASTNode.TEXT_ELEMENT to "TEXT_ELEMENT",
                ASTNode.MEMBER_REF to "MEMBER_REF",
                ASTNode.METHOD_REF to "METHOD_REF",
                ASTNode.METHOD_REF_PARAMETER to "METHOD_REF_PARAMETER",
                ASTNode.ENHANCED_FOR_STATEMENT to "ENHANCED_FOR_STATEMENT",
                ASTNode.ENUM_DECLARATION to "ENUM_DECLARATION",
                ASTNode.ENUM_CONSTANT_DECLARATION to "ENUM_CONSTANT_DECLARATION",
                ASTNode.TYPE_PARAMETER to "TYPE_PARAMETER",
                ASTNode.PARAMETERIZED_TYPE to "PARAMETERIZED_TYPE",
                ASTNode.QUALIFIED_TYPE to "QUALIFIED_TYPE",
                ASTNode.WILDCARD_TYPE to "WILDCARD_TYPE",
                ASTNode.NORMAL_ANNOTATION to "NORMAL_ANNOTATION",
                ASTNode.MARKER_ANNOTATION to "MARKER_ANNOTATION",
                ASTNode.SINGLE_MEMBER_ANNOTATION to "SINGLE_MEMBER_ANNOTATION",
                ASTNode.MEMBER_VALUE_PAIR to "MEMBER_VALUE_PAIR",
                ASTNode.ANNOTATION_TYPE_DECLARATION to "ANNOTATION_TYPE_DECLARATION",
                ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION to "ANNOTATION_TYPE_MEMBER_DECLARATION",
                ASTNode.UNION_TYPE to "UNION_TYPE",
                ASTNode.DIMENSION to "DIMENSION",
                ASTNode.LAMBDA_EXPRESSION to "LAMBDA_EXPRESSION",
                ASTNode.INTERSECTION_TYPE to "INTERSECTION_TYPE",
                ASTNode.NAME_QUALIFIED_TYPE to "NAME_QUALIFIED_TYPE",
                ASTNode.CREATION_REFERENCE to "CREATION_REFERENCE",
                ASTNode.EXPRESSION_METHOD_REFERENCE to "EXPRESSION_METHOD_REFERENCE",
                ASTNode.SUPER_METHOD_REFERENCE to "SUPER_METHOD_REFERENCE",
                ASTNode.TYPE_METHOD_REFERENCE to "TYPE_METHOD_REFERENCE",
            ),
            synonymTokenTypes = emptyMap()
        )
        val TOKEN = nodeTypes.getTokenType("TOKEN")!!
    }
}

private object CatVisitor : AstVisitor<String> {
    override fun visitRule(node: AstRuleNode): String = visit(node)
    override fun visitTerminal(node: AstTerminalNode): String = node.token
    override fun visit(node: AstNode): String = node.children?.joinToString("") { it.accept(this) } ?: ""
}