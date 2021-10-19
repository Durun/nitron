package com.github.durun.nitron.core.parser.jdt

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstRuleNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.type.NodeTypePool
import com.github.durun.nitron.core.ast.visitor.AstVisitor
import com.github.durun.nitron.core.parser.NitronParser
import com.github.durun.nitron.core.parser.NitronParsers
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import java.io.Reader

fun init(version: String = JavaCore.VERSION_16) = NitronParsers.jdt(version)

@Suppress("UNUSED")
fun NitronParsers.jdt(version: String = JavaCore.VERSION_16): NitronParser = JdtParser(version)

class JdtParser(version: String = JavaCore.VERSION_16) : NitronParser {
    override val nodeTypes: NodeTypePool = Companion.nodeTypes

    private val parser = ThreadLocal.withInitial {
        ASTParser.newParser(AST.JLS16)
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
    }

    override fun parse(reader: Reader): AstNode {
        val source = reader.readText().replace(Regex("\r\n|\r|\n"), "\n")
        parser.get().setSource(source.toCharArray())
        val root = parser.get().createAST(null)
        val converter = AstConvertVisitor()
        root.accept(converter)
        return converter.result
    }

    companion object {
        val nodeTypes: NodeTypePool = NodeTypePool.of(
            "java",
            tokenTypes = mapOf(
                -1 to "TOKEN",
                ASTNode.BOOLEAN_LITERAL to "BOOLEAN_LITERAL",
                ASTNode.CHARACTER_LITERAL to "CHARACTER_LITERAL",
                ASTNode.NULL_LITERAL to "NULL_LITERAL",
                ASTNode.NUMBER_LITERAL to "NUMBER_LITERAL",
                ASTNode.SIMPLE_NAME to "SIMPLE_NAME",
                ASTNode.STRING_LITERAL to "STRING_LITERAL",
                ASTNode.LINE_COMMENT to "LINE_COMMENT",
                ASTNode.BLOCK_COMMENT to "BLOCK_COMMENT",
                ASTNode.MODIFIER to "MODIFIER",
                ASTNode.MODULE_MODIFIER to "MODULE_MODIFIER",
                ASTNode.TEXT_BLOCK to "TEXT_BLOCK",
                ASTNode.TEXT_ELEMENT to "TEXT_ELEMENT",
            ),
            ruleTypes = mapOf(
                ASTNode.SIMPLE_TYPE to "SIMPLE_TYPE",
                ASTNode.PRIMITIVE_TYPE to "PRIMITIVE_TYPE",
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
                ASTNode.MODULE_DECLARATION to "MODULE_DECLARATION",
                ASTNode.MODULE_QUALIFIED_NAME to "MODULE_QUALIFIED_NAME",
                ASTNode.RECORD_DECLARATION to "RECORD_DECLARATION",
                ASTNode.EXPORTS_DIRECTIVE to "EXPORTS_DIRECTIVE",
                ASTNode.OPENS_DIRECTIVE to "OPENS_DIRECTIVE",
                ASTNode.PROVIDES_DIRECTIVE to "PROVIDES_DIRECTIVE",
                ASTNode.USES_DIRECTIVE to "USES_DIRECTIVE",
                ASTNode.REQUIRES_DIRECTIVE to "REQUIRES_DIRECTIVE",
                ASTNode.PATTERN_INSTANCEOF_EXPRESSION to "PATTERN_INSTANCEOF_EXPRESSION",
                ASTNode.SWITCH_EXPRESSION to "SWITCH_EXPRESSION",
                ASTNode.TYPE_LITERAL to "TYPE_LITERAL",
                ASTNode.YIELD_STATEMENT to "YIELD_STATEMENT",
            ),
            synonymTokenTypes = emptyMap()
        )
        val TOKEN = nodeTypes.getTokenType("TOKEN")!!
    }

    object TokenTypes {
        val BOOLEAN_LITERAL = nodeTypes.getTokenType(ASTNode.BOOLEAN_LITERAL)!!
        val CHARACTER_LITERAL = nodeTypes.getTokenType(ASTNode.CHARACTER_LITERAL)!!
        val NULL_LITERAL = nodeTypes.getTokenType(ASTNode.NULL_LITERAL)!!
        val NUMBER_LITERAL = nodeTypes.getTokenType(ASTNode.NUMBER_LITERAL)!!
        val SIMPLE_NAME = nodeTypes.getTokenType(ASTNode.SIMPLE_NAME)!!
        val STRING_LITERAL = nodeTypes.getTokenType(ASTNode.STRING_LITERAL)!!
        val LINE_COMMENT = nodeTypes.getTokenType(ASTNode.LINE_COMMENT)!!
        val BLOCK_COMMENT = nodeTypes.getTokenType(ASTNode.BLOCK_COMMENT)!!
        val MODIFIER = nodeTypes.getTokenType(ASTNode.MODIFIER)!!
        val MODULE_MODIFIER = nodeTypes.getTokenType(ASTNode.MODULE_MODIFIER)!!
        val TEXT_BLOCK = nodeTypes.getTokenType(ASTNode.TEXT_BLOCK)!!
        val TEXT_ELEMENT = nodeTypes.getTokenType(ASTNode.TEXT_ELEMENT)!!
    }

    object RuleTypes {
        val SIMPLE_TYPE = nodeTypes.getRuleType(ASTNode.SIMPLE_TYPE)!!
        val PRIMITIVE_TYPE = nodeTypes.getRuleType(ASTNode.PRIMITIVE_TYPE)!!
        val ANONYMOUS_CLASS_DECLARATION = nodeTypes.getRuleType(ASTNode.ANONYMOUS_CLASS_DECLARATION)!!
        val ARRAY_ACCESS = nodeTypes.getRuleType(ASTNode.ARRAY_ACCESS)!!
        val ARRAY_CREATION = nodeTypes.getRuleType(ASTNode.ARRAY_CREATION)!!
        val ARRAY_INITIALIZER = nodeTypes.getRuleType(ASTNode.ARRAY_INITIALIZER)!!
        val ARRAY_TYPE = nodeTypes.getRuleType(ASTNode.ARRAY_TYPE)!!
        val ASSERT_STATEMENT = nodeTypes.getRuleType(ASTNode.ASSERT_STATEMENT)!!
        val ASSIGNMENT = nodeTypes.getRuleType(ASTNode.ASSIGNMENT)!!
        val BLOCK = nodeTypes.getRuleType(ASTNode.BLOCK)!!
        val BREAK_STATEMENT = nodeTypes.getRuleType(ASTNode.BREAK_STATEMENT)!!
        val CAST_EXPRESSION = nodeTypes.getRuleType(ASTNode.CAST_EXPRESSION)!!
        val CATCH_CLAUSE = nodeTypes.getRuleType(ASTNode.CATCH_CLAUSE)!!
        val CLASS_INSTANCE_CREATION = nodeTypes.getRuleType(ASTNode.CLASS_INSTANCE_CREATION)!!
        val COMPILATION_UNIT = nodeTypes.getRuleType(ASTNode.COMPILATION_UNIT)!!
        val CONDITIONAL_EXPRESSION = nodeTypes.getRuleType(ASTNode.CONDITIONAL_EXPRESSION)!!
        val CONSTRUCTOR_INVOCATION = nodeTypes.getRuleType(ASTNode.CONSTRUCTOR_INVOCATION)!!
        val CONTINUE_STATEMENT = nodeTypes.getRuleType(ASTNode.CONTINUE_STATEMENT)!!
        val DO_STATEMENT = nodeTypes.getRuleType(ASTNode.DO_STATEMENT)!!
        val EMPTY_STATEMENT = nodeTypes.getRuleType(ASTNode.EMPTY_STATEMENT)!!
        val EXPRESSION_STATEMENT = nodeTypes.getRuleType(ASTNode.EXPRESSION_STATEMENT)!!
        val FIELD_ACCESS = nodeTypes.getRuleType(ASTNode.FIELD_ACCESS)!!
        val FIELD_DECLARATION = nodeTypes.getRuleType(ASTNode.FIELD_DECLARATION)!!
        val FOR_STATEMENT = nodeTypes.getRuleType(ASTNode.FOR_STATEMENT)!!
        val IF_STATEMENT = nodeTypes.getRuleType(ASTNode.IF_STATEMENT)!!
        val IMPORT_DECLARATION = nodeTypes.getRuleType(ASTNode.IMPORT_DECLARATION)!!
        val INFIX_EXPRESSION = nodeTypes.getRuleType(ASTNode.INFIX_EXPRESSION)!!
        val INITIALIZER = nodeTypes.getRuleType(ASTNode.INITIALIZER)!!
        val JAVADOC = nodeTypes.getRuleType(ASTNode.JAVADOC)!!
        val LABELED_STATEMENT = nodeTypes.getRuleType(ASTNode.LABELED_STATEMENT)!!
        val METHOD_DECLARATION = nodeTypes.getRuleType(ASTNode.METHOD_DECLARATION)!!
        val METHOD_INVOCATION = nodeTypes.getRuleType(ASTNode.METHOD_INVOCATION)!!
        val PACKAGE_DECLARATION = nodeTypes.getRuleType(ASTNode.PACKAGE_DECLARATION)!!
        val PARENTHESIZED_EXPRESSION = nodeTypes.getRuleType(ASTNode.PARENTHESIZED_EXPRESSION)!!
        val POSTFIX_EXPRESSION = nodeTypes.getRuleType(ASTNode.POSTFIX_EXPRESSION)!!
        val PREFIX_EXPRESSION = nodeTypes.getRuleType(ASTNode.PREFIX_EXPRESSION)!!
        val QUALIFIED_NAME = nodeTypes.getRuleType(ASTNode.QUALIFIED_NAME)!!
        val RETURN_STATEMENT = nodeTypes.getRuleType(ASTNode.RETURN_STATEMENT)!!
        val SINGLE_VARIABLE_DECLARATION = nodeTypes.getRuleType(ASTNode.SINGLE_VARIABLE_DECLARATION)!!
        val SUPER_CONSTRUCTOR_INVOCATION = nodeTypes.getRuleType(ASTNode.SUPER_CONSTRUCTOR_INVOCATION)!!
        val SUPER_FIELD_ACCESS = nodeTypes.getRuleType(ASTNode.SUPER_FIELD_ACCESS)!!
        val SUPER_METHOD_INVOCATION = nodeTypes.getRuleType(ASTNode.SUPER_METHOD_INVOCATION)!!
        val SWITCH_CASE = nodeTypes.getRuleType(ASTNode.SWITCH_CASE)!!
        val SWITCH_STATEMENT = nodeTypes.getRuleType(ASTNode.SWITCH_STATEMENT)!!
        val SYNCHRONIZED_STATEMENT = nodeTypes.getRuleType(ASTNode.SYNCHRONIZED_STATEMENT)!!
        val THIS_EXPRESSION = nodeTypes.getRuleType(ASTNode.THIS_EXPRESSION)!!
        val THROW_STATEMENT = nodeTypes.getRuleType(ASTNode.THROW_STATEMENT)!!
        val TRY_STATEMENT = nodeTypes.getRuleType(ASTNode.TRY_STATEMENT)!!
        val TYPE_DECLARATION = nodeTypes.getRuleType(ASTNode.TYPE_DECLARATION)!!
        val TYPE_DECLARATION_STATEMENT = nodeTypes.getRuleType(ASTNode.TYPE_DECLARATION_STATEMENT)!!
        val VARIABLE_DECLARATION_EXPRESSION = nodeTypes.getRuleType(ASTNode.VARIABLE_DECLARATION_EXPRESSION)!!
        val VARIABLE_DECLARATION_FRAGMENT = nodeTypes.getRuleType(ASTNode.VARIABLE_DECLARATION_FRAGMENT)!!
        val VARIABLE_DECLARATION_STATEMENT = nodeTypes.getRuleType(ASTNode.VARIABLE_DECLARATION_STATEMENT)!!
        val WHILE_STATEMENT = nodeTypes.getRuleType(ASTNode.WHILE_STATEMENT)!!
        val INSTANCEOF_EXPRESSION = nodeTypes.getRuleType(ASTNode.INSTANCEOF_EXPRESSION)!!
        val TAG_ELEMENT = nodeTypes.getRuleType(ASTNode.TAG_ELEMENT)!!
        val TEXT_ELEMENT = nodeTypes.getRuleType(ASTNode.TEXT_ELEMENT)!!
        val MEMBER_REF = nodeTypes.getRuleType(ASTNode.MEMBER_REF)!!
        val METHOD_REF = nodeTypes.getRuleType(ASTNode.METHOD_REF)!!
        val METHOD_REF_PARAMETER = nodeTypes.getRuleType(ASTNode.METHOD_REF_PARAMETER)!!
        val ENHANCED_FOR_STATEMENT = nodeTypes.getRuleType(ASTNode.ENHANCED_FOR_STATEMENT)!!
        val ENUM_DECLARATION = nodeTypes.getRuleType(ASTNode.ENUM_DECLARATION)!!
        val ENUM_CONSTANT_DECLARATION = nodeTypes.getRuleType(ASTNode.ENUM_CONSTANT_DECLARATION)!!
        val TYPE_PARAMETER = nodeTypes.getRuleType(ASTNode.TYPE_PARAMETER)!!
        val PARAMETERIZED_TYPE = nodeTypes.getRuleType(ASTNode.PARAMETERIZED_TYPE)!!
        val QUALIFIED_TYPE = nodeTypes.getRuleType(ASTNode.QUALIFIED_TYPE)!!
        val WILDCARD_TYPE = nodeTypes.getRuleType(ASTNode.WILDCARD_TYPE)!!
        val NORMAL_ANNOTATION = nodeTypes.getRuleType(ASTNode.NORMAL_ANNOTATION)!!
        val MARKER_ANNOTATION = nodeTypes.getRuleType(ASTNode.MARKER_ANNOTATION)!!
        val SINGLE_MEMBER_ANNOTATION = nodeTypes.getRuleType(ASTNode.SINGLE_MEMBER_ANNOTATION)!!
        val MEMBER_VALUE_PAIR = nodeTypes.getRuleType(ASTNode.MEMBER_VALUE_PAIR)!!
        val ANNOTATION_TYPE_DECLARATION = nodeTypes.getRuleType(ASTNode.ANNOTATION_TYPE_DECLARATION)!!
        val ANNOTATION_TYPE_MEMBER_DECLARATION = nodeTypes.getRuleType(ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION)!!
        val UNION_TYPE = nodeTypes.getRuleType(ASTNode.UNION_TYPE)!!
        val DIMENSION = nodeTypes.getRuleType(ASTNode.DIMENSION)!!
        val LAMBDA_EXPRESSION = nodeTypes.getRuleType(ASTNode.LAMBDA_EXPRESSION)!!
        val INTERSECTION_TYPE = nodeTypes.getRuleType(ASTNode.INTERSECTION_TYPE)!!
        val NAME_QUALIFIED_TYPE = nodeTypes.getRuleType(ASTNode.NAME_QUALIFIED_TYPE)!!
        val CREATION_REFERENCE = nodeTypes.getRuleType(ASTNode.CREATION_REFERENCE)!!
        val EXPRESSION_METHOD_REFERENCE = nodeTypes.getRuleType(ASTNode.EXPRESSION_METHOD_REFERENCE)!!
        val SUPER_METHOD_REFERENCE = nodeTypes.getRuleType(ASTNode.SUPER_METHOD_REFERENCE)!!
        val TYPE_METHOD_REFERENCE = nodeTypes.getRuleType(ASTNode.TYPE_METHOD_REFERENCE)!!
        val MODULE_DECLARATION = nodeTypes.getRuleType(ASTNode.MODULE_DECLARATION)!!
        val MODULE_QUALIFIED_NAME = nodeTypes.getRuleType(ASTNode.MODULE_QUALIFIED_NAME)!!
        val RECORD_DECLARATION = nodeTypes.getRuleType(ASTNode.RECORD_DECLARATION)!!
        val EXPORTS_DIRECTIVE = nodeTypes.getRuleType(ASTNode.EXPORTS_DIRECTIVE)!!
        val OPENS_DIRECTIVE = nodeTypes.getRuleType(ASTNode.OPENS_DIRECTIVE)!!
        val PROVIDES_DIRECTIVE = nodeTypes.getRuleType(ASTNode.PROVIDES_DIRECTIVE)!!
        val USES_DIRECTIVE = nodeTypes.getRuleType(ASTNode.USES_DIRECTIVE)!!
        val REQUIRES_DIRECTIVE = nodeTypes.getRuleType(ASTNode.REQUIRES_DIRECTIVE)!!
        val PATTERN_INSTANCEOF_EXPRESSION = nodeTypes.getRuleType(ASTNode.PATTERN_INSTANCEOF_EXPRESSION)!!
        val SWITCH_EXPRESSION = nodeTypes.getRuleType(ASTNode.SWITCH_EXPRESSION)!!
        val TYPE_LITERAL = nodeTypes.getRuleType(ASTNode.TYPE_LITERAL)!!
        val YIELD_STATEMENT = nodeTypes.getRuleType(ASTNode.YIELD_STATEMENT)!!
    }
}

private object CatVisitor : AstVisitor<String> {
    override fun visitRule(node: AstRuleNode): String = visit(node)
    override fun visitTerminal(node: AstTerminalNode): String = node.token
    override fun visit(node: AstNode): String = node.children?.joinToString("") { it.accept(this) } ?: ""
}