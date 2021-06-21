package io.github.durun.nitron.core.parser.jdt

import io.github.durun.nitron.core.ast.node.AstNode
import io.github.durun.nitron.core.ast.node.AstTerminalNode
import io.github.durun.nitron.core.ast.node.BasicAstRuleNode
import io.github.durun.nitron.core.ast.type.NodeTypePool
import io.github.durun.nitron.core.ast.type.RuleType
import io.github.durun.nitron.core.ast.type.TokenType
import io.github.durun.nitron.core.parser.AstBuilder
import io.github.durun.nitron.core.parser.AstBuilders
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import java.io.Reader


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
        parser.setSource(reader.readText().toCharArray())
        val root = parser.createAST(null)
        val visitor = BuildVisitor()
        root.accept(visitor)
        return visitor.result!!
    }

    private class BuildVisitor : ASTVisitor() {
        var result: AstNode? = null
            private set
        private val stack: MutableList<AstNode> = mutableListOf()

        override fun preVisit(node: ASTNode) {
            val newNode =
                when (val type = nodeTypes.getRuleType(node.nodeType) ?: nodeTypes.getTokenType(node.nodeType)) {
                    is RuleType -> BasicAstRuleNode(type, mutableListOf())
                    is TokenType -> AstTerminalNode(node.toString(), type, 0)
                    else -> throw Exception()
                }
            val parent = stack.lastOrNull()
            if (parent is BasicAstRuleNode) {
                parent.children += newNode
            }
            stack += newNode
        }

        override fun postVisit(node: ASTNode) {
            result = stack.removeLast()
        }
    }

    companion object {
        val nodeTypes: NodeTypePool = NodeTypePool.of(
            "java",
            tokenTypes = mapOf(
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
    }
}
