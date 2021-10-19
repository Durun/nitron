package com.github.durun.nitron.core.parser.jdt

import com.github.durun.nitron.core.ast.node.AstNode
import com.github.durun.nitron.core.ast.node.AstTerminalNode
import com.github.durun.nitron.core.ast.node.BasicAstRuleNode
import com.github.durun.nitron.core.ast.type.RuleType
import com.github.durun.nitron.core.ast.type.TokenType
import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.Annotation
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper
import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil


class AstConvertVisitor : ASTVisitor() {
    companion object {
        private val TOKEN = JdtParser.TOKEN
        private val RuleTypes = JdtParser.RuleTypes
        private val TokenTypes = JdtParser.TokenTypes
    }

    lateinit var result: BasicAstRuleNode
    private lateinit var unit: CompilationUnit
    private lateinit var cursor: MutableList<AstNode>

    private fun getLineStart(node: ASTNode): Int = unit.getLineNumber(node.startPosition)
    private fun getLineEnd(node: ASTNode): Int = unit.getLineNumber(node.startPosition + node.length - 1)
    private fun setCursor(node: BasicAstRuleNode) {
        cursor = node.children
    }

    private fun <R> startRule(type: RuleType, body: () -> R): R {
        val ast = BasicAstRuleNode(type, mutableListOf())
        append(ast)
        val prevCursor = cursor
        setCursor(ast)
        val result = body()
        cursor = prevCursor
        return result
    }

    private fun startParse(node: CompilationUnit) {
        unit = node
        val ast = BasicAstRuleNode(RuleTypes.COMPILATION_UNIT, mutableListOf())
        setCursor(ast)
        result = ast
    }

    private fun append(text: String, lineNo: Int = 1) {
        val trimmed = text.replace("\n", "").trim()
        if (trimmed.isNotEmpty()) append(AstTerminalNode(text, TOKEN, lineNo))
    }

    private fun append(ast: AstNode) {
        cursor.add(ast)
    }

    private fun append(text: String, type: TokenType, lineNo: Int) {
        val ast = AstTerminalNode(text, type, lineNo)
        append(ast)
    }

    private fun appendModifiers(ext: List<*>) {
        ext.forEach {
            val p = it as ASTNode
            p.accept(this)
        }
    }

    private fun appendTypes(types: List<*>, prefix: String, startLine: Int) {
        if (types.isNotEmpty()) {
            var lineNo = startLine
            append(prefix, lineNo)
            var type = types[0] as Type
            type.accept(this)
            lineNo = getLineEnd(type)
            var i = 1
            val l = types.size
            while (i < l) {
                append(",", lineNo)
                type = types[i] as Type
                type.accept(this)
                lineNo = getLineEnd(type)
                ++i
            }
        }
    }

    private fun visitReferenceTypeArguments(typeArguments: List<*>, startLine: Int, endLine: Int) {
        append("::", startLine)
        if (typeArguments.isNotEmpty()) {
            append("<", startLine)
            val it = typeArguments.iterator()
            while (it.hasNext()) {
                val t = it.next() as Type
                t.accept(this)
                if (it.hasNext()) {
                    append(",", getLineEnd(t))
                }
            }
            append(">", endLine)
        }
    }

    private fun visitTypeAnnotations(node: AnnotatableType) {
        visitAnnotationsList(node.annotations())
    }

    private fun visitAnnotationsList(annotations: List<*>) {
        annotations.forEach {
            val annotation = it as Annotation
            annotation.accept(this)
        }
    }

    override fun visit(node: AnnotationTypeDeclaration): Boolean {
        startRule(RuleTypes.ANNOTATION_TYPE_DECLARATION) {
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            val lineNo = getLineStart(node.name)
            append("@", lineNo)
            append("interface", lineNo)
            node.name.accept(this)
            append("{", lineNo)
            node.bodyDeclarations().forEach {
                val d = it as BodyDeclaration
                d.accept(this)
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: AnnotationTypeMemberDeclaration): Boolean {
        startRule(RuleTypes.ANNOTATION_TYPE_MEMBER_DECLARATION) {
            val lineNo = getLineStart(node)
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            node.type.accept(this)
            node.name.accept(this)
            append("(", lineNo)
            append(")", lineNo)
            node.default?.let {
                append("default", lineNo)
                it.accept(this)
            }
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: AnonymousClassDeclaration): Boolean {
        startRule(RuleTypes.ANONYMOUS_CLASS_DECLARATION) {
            append("{", getLineStart(node))
            node.bodyDeclarations().forEach {
                val b = it as BodyDeclaration
                b.accept(this)
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ArrayAccess): Boolean {
        startRule(RuleTypes.ARRAY_ACCESS) {
            val lineNo = getLineEnd(node)
            node.array.accept(this)
            append("[", lineNo)
            node.index.accept(this)
            append("]", lineNo)
        }
        return false
    }

    override fun visit(node: ArrayCreation): Boolean {
        startRule(RuleTypes.ARRAY_CREATION) {
            val lineNo = getLineStart(node)
            append("new", lineNo)
            val at = node.type
            var dims = at.dimensions
            val elementType = at.elementType
            elementType.accept(this)
            node.dimensions().forEach {
                append("[", lineNo)
                val e = it as Expression
                e.accept(this)
                append("]", lineNo)
                dims--
            }
            // add empty "[]" for each extra array dimension
            for (i in 0 until dims) {
                append("[", lineNo)
                append("]", lineNo)
            }
            node.initializer?.accept(this)
        }
        return false
    }

    override fun visit(node: ArrayInitializer): Boolean {
        startRule(RuleTypes.ARRAY_INITIALIZER) {
            append("{", getLineStart(node))
            val it: Iterator<*> = node.expressions().iterator()
            while (it.hasNext()) {
                val e = it.next() as Expression
                e.accept(this)
                if (it.hasNext()) {
                    append(",", getLineEnd(e))
                }
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ArrayType): Boolean {
        startRule(RuleTypes.ARRAY_TYPE) {
            node.elementType.accept(this)
            node.dimensions().forEach {
                val d = it as Dimension
                d.accept(this)
            }
        }
        return false
    }

    override fun visit(node: AssertStatement): Boolean {
        startRule(RuleTypes.ASSERT_STATEMENT) {
            append("assert ", getLineStart(node))
            node.expression.accept(this)
            node.message?.let {
                append(" : ", getLineStart(it))
                it.accept(this)
            }
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: Assignment): Boolean {
        startRule(RuleTypes.ASSIGNMENT) {
            node.leftHandSide.accept(this)
            append(node.operator.toString(), getLineStart(node))
            node.rightHandSide.accept(this)
        }
        return false
    }

    override fun visit(node: Block): Boolean {
        startRule(RuleTypes.BLOCK) {
            append("{", getLineStart(node))
            node.statements().forEach {
                val s = it as Statement
                s.accept(this)
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: BooleanLiteral): Boolean {
        val text = if (node.booleanValue()) "true" else "false"
        append(text, TokenTypes.BOOLEAN_LITERAL, getLineStart(node))
        return false
    }

    override fun visit(node: BreakStatement): Boolean {
        startRule(RuleTypes.BREAK_STATEMENT) {
            append("break", getLineStart(node))
            node.label?.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: CastExpression): Boolean {
        startRule(RuleTypes.CAST_EXPRESSION) {
            val lineNo = getLineStart(node)
            append("(", lineNo)
            node.type.accept(this)
            append(")", lineNo)
            node.expression.accept(this)
        }
        return false
    }

    override fun visit(node: CatchClause): Boolean {
        startRule(RuleTypes.CATCH_CLAUSE) {
            val lineNo = getLineStart(node)
            append("catch", lineNo)
            append("(", lineNo)
            node.exception.accept(this)
            append(")", getLineEnd(node.exception))
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: CharacterLiteral): Boolean {
        append(node.escapedValue, TokenTypes.CHARACTER_LITERAL, getLineStart(node))
        return false
    }

    override fun visit(node: ClassInstanceCreation): Boolean {
        startRule(RuleTypes.CLASS_INSTANCE_CREATION) {
            val lineNo =
                node.expression?.let {
                    it.accept(this)
                    val l = getLineEnd(it)
                    append(".", l)
                    l
                } ?: getLineStart(node)
            append("new", lineNo)
            if (node.typeArguments().isNotEmpty()) {
                append("<", lineNo)
                val it: Iterator<*> = node.typeArguments().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    if (it.hasNext()) {
                        append(",", lineNo)
                    }
                }
                append(">", lineNo)
            }
            node.type.accept(this)
            append("(", lineNo)
            val it: Iterator<*> = node.arguments().iterator()
            while (it.hasNext()) {
                val e = it.next() as Expression
                e.accept(this)
                if (it.hasNext()) {
                    append(",", lineNo)
                }
            }
            append(")", lineNo)
            node.anonymousClassDeclaration?.accept(this)
        }
        return false
    }

    override fun visit(node: CompilationUnit): Boolean {
        startParse(node)
        node.module?.accept(this)
        node.getPackage()?.accept(this)
        node.imports().forEach {
            val d = it as ImportDeclaration
            d.accept(this)
        }
        node.types().forEach {
            val d = it as AbstractTypeDeclaration
            d.accept(this)
        }
        return false
    }

    override fun visit(node: ConditionalExpression): Boolean {
        startRule(RuleTypes.CONDITIONAL_EXPRESSION) {
            node.expression.accept(this)
            append(" ? ", getLineStart(node.thenExpression))
            node.thenExpression.accept(this)
            append(" : ", getLineStart(node.elseExpression))
            node.elseExpression.accept(this)
        }
        return false
    }

    override fun visit(node: ConstructorInvocation): Boolean {
        startRule(RuleTypes.CONSTRUCTOR_INVOCATION) {
            val lineNo = getLineStart(node)
            if (node.typeArguments().isNotEmpty()) {
                append("<", lineNo)
                val it: Iterator<*> = node.typeArguments().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    if (it.hasNext()) {
                        append(",", lineNo)
                    }
                }
                append(">", lineNo)
            }

            append("this(", lineNo)
            val it: Iterator<*> = node.arguments().iterator()
            while (it.hasNext()) {
                val e = it.next() as Expression
                e.accept(this)
                if (it.hasNext()) {
                    append(",", getLineEnd(e))
                }
            }
            val endLineNo = getLineStart(node)
            append(")", endLineNo)
            append(";", endLineNo)
        }
        return false
    }

    override fun visit(node: ContinueStatement): Boolean {
        startRule(RuleTypes.CONTINUE_STATEMENT) {
            append("continue", getLineStart(node))
            node.label?.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: CreationReference): Boolean {
        startRule(RuleTypes.CREATION_REFERENCE) {
            node.type.accept(this)
            visitReferenceTypeArguments(node.typeArguments(), getLineEnd(node.type), getLineEnd(node))
            append("new", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: Dimension): Boolean {
        startRule(RuleTypes.DIMENSION) {
            val annotations = node.annotations()
            visitAnnotationsList(annotations)
            val lineNo = getLineEnd(node)
            append("[", lineNo)
            append("]", lineNo)
        }
        return false
    }

    override fun visit(node: DoStatement): Boolean {
        startRule(RuleTypes.DO_STATEMENT) {
            append("do", getLineStart(node))
            node.body.accept(this)
            val lineNo = getLineEnd(node.body)
            append("while", lineNo)
            append("(", lineNo)
            node.expression.accept(this)
            append(")", getLineEnd(node.expression))
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: EmptyStatement): Boolean {
        startRule(RuleTypes.EMPTY_STATEMENT) {
            append(";", getLineStart(node))
        }
        return false
    }

    override fun visit(node: EnhancedForStatement): Boolean {
        startRule(RuleTypes.ENHANCED_FOR_STATEMENT) {
            val lineNo = getLineStart(node)
            append("for", lineNo)
            append("(", lineNo)
            node.parameter.accept(this)
            append(":", getLineStart(node.expression))
            node.expression.accept(this)
            append(")", getLineEnd(node.expression))
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: EnumConstantDeclaration): Boolean {
        startRule(RuleTypes.ENUM_CONSTANT_DECLARATION) {
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            node.name.accept(this)
            if (node.arguments().isNotEmpty()) {
                append("(", getLineStart(node))
                val it: Iterator<*> = node.arguments().iterator()
                while (it.hasNext()) {
                    val e = it.next() as Expression
                    e.accept(this)
                    if (it.hasNext()) {
                        append(",", getLineEnd(e))
                    }
                }
                append(")", getLineEnd(node))
            }
            node.anonymousClassDeclaration?.accept(this)
        }
        return false
    }

    override fun visit(node: EnumDeclaration): Boolean {
        startRule(RuleTypes.ENUM_DECLARATION) {
            //node.javadoc?.accept(this)
            var lineNo = getLineStart(node)
            appendModifiers(node.modifiers())
            append("enum", lineNo)
            node.name.accept(this)
            if (node.superInterfaceTypes().isNotEmpty()) {
                append("implements", lineNo)
                val it: Iterator<*> = node.superInterfaceTypes().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) {
                        append(",", lineNo)
                    }
                }
            }
            append("{", lineNo)
            val it: Iterator<*> = node.enumConstants().iterator()
            while (it.hasNext()) {
                val d = it.next() as EnumConstantDeclaration
                d.accept(this)
                lineNo = getLineEnd(d)
                // enum constant declarations do not include punctuation
                if (it.hasNext()) {
                    // enum constant declarations are separated by commas
                    append(",", lineNo)
                }
            }
            if (node.bodyDeclarations().isNotEmpty()) {
                append(";", lineNo)
                node.bodyDeclarations().forEach {
                    val d = it as BodyDeclaration
                    d.accept(this)
                }
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ExportsDirective): Boolean {
        return startRule(RuleTypes.EXPORTS_DIRECTIVE) {
            visit(node, "exports")
        }
    }

    override fun visit(node: ExpressionMethodReference): Boolean {
        startRule(RuleTypes.EXPRESSION_METHOD_REFERENCE) {
            node.expression.accept(this)
            visitReferenceTypeArguments(node.typeArguments(), getLineEnd(node.expression), getLineStart(node.name))
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: ExpressionStatement): Boolean {
        startRule(RuleTypes.EXPRESSION_STATEMENT) {
            node.expression.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: FieldAccess): Boolean {
        startRule(RuleTypes.FIELD_ACCESS) {
            node.expression.accept(this)
            append(".", getLineStart(node.name))
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: FieldDeclaration): Boolean {
        startRule(RuleTypes.FIELD_DECLARATION) {
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            node.type.accept(this)
            val it: Iterator<*> = node.fragments().iterator()
            while (it.hasNext()) {
                val f = it.next() as VariableDeclarationFragment
                f.accept(this)
                if (it.hasNext()) append(",", getLineEnd(f))
            }
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ForStatement): Boolean {
        startRule(RuleTypes.FOR_STATEMENT) {
            var lineNo = getLineStart(node)
            append("for", lineNo)
            append("(", lineNo)
            run {
                val it: Iterator<*> = node.initializers().iterator()
                while (it.hasNext()) {
                    val e = it.next() as Expression
                    e.accept(this)
                    lineNo = getLineEnd(e)
                    if (it.hasNext()) append(",", lineNo)
                }
            }
            append(";", lineNo)
            node.expression?.let {
                it.accept(this)
                lineNo = getLineEnd(it)
            }
            append(";", lineNo)
            val it: Iterator<*> = node.updaters().iterator()
            while (it.hasNext()) {
                val e = it.next() as Expression
                e.accept(this)
                lineNo = getLineEnd(e)
                if (it.hasNext()) append(",", lineNo)
            }
            append(")", lineNo)
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: IfStatement): Boolean {
        startRule(RuleTypes.IF_STATEMENT) {
            val lineNo = getLineStart(node)
            append("if", lineNo)
            append("(", lineNo)
            node.expression.accept(this)
            append(")", getLineEnd(node.expression))
            node.thenStatement.accept(this)
            node.elseStatement?.let {
                append("else", getLineStart(it))
                it.accept(this)
            }
        }
        return false
    }

    override fun visit(node: ImportDeclaration): Boolean {
        startRule(RuleTypes.IMPORT_DECLARATION) {
            val lineNo = getLineStart(node)
            append("import", lineNo)
            if (node.isStatic) {
                append("static", lineNo)
            }
            node.name.accept(this)
            if (node.isOnDemand) {
                append(".", lineNo)
                append("*", lineNo)
            }
            append(";", lineNo)
        }
        return false
    }

    override fun visit(node: InfixExpression): Boolean {
        startRule(RuleTypes.INFIX_EXPRESSION) {
            node.leftOperand.accept(this)
            append(node.operator.toString(), getLineStart(node.rightOperand))
            node.rightOperand.accept(this)
            node.extendedOperands().forEach {
                val e = it as Expression
                append(node.operator.toString(), getLineStart(e))
                e.accept(this)
            }
        }
        return false
    }

    override fun visit(node: Initializer): Boolean {
        startRule(RuleTypes.INITIALIZER) {
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: InstanceofExpression): Boolean {
        startRule(RuleTypes.INSTANCEOF_EXPRESSION) {
            node.leftOperand.accept(this)
            append("instanceof", getLineStart(node.rightOperand))
            node.rightOperand.accept(this)
        }
        return false
    }

    override fun visit(node: PatternInstanceofExpression): Boolean {
        startRule(RuleTypes.PATTERN_INSTANCEOF_EXPRESSION) {
            node.leftOperand.accept(this)
            append("instanceof", getLineStart(node.rightOperand))
            node.rightOperand.accept(this)
        }
        return false
    }

    override fun visit(node: IntersectionType): Boolean {
        startRule(RuleTypes.INTERSECTION_TYPE) {
            val it: Iterator<*> = node.types().iterator()
            while (it.hasNext()) {
                val t = it.next() as Type
                t.accept(this)
                if (it.hasNext()) {
                    append("&", getLineEnd(t))
                }
            }
        }
        return false
    }

    override fun visit(node: Javadoc): Boolean {
        startRule(RuleTypes.JAVADOC) {
            append("/**", getLineStart(node))
            node.tags().forEach {
                val e = it as ASTNode
                e.accept(this)
            }
            append("*/", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: LabeledStatement): Boolean {
        startRule(RuleTypes.LABELED_STATEMENT) {
            node.label.accept(this)
            append(":", getLineEnd(node.label))
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: LambdaExpression): Boolean {
        startRule(RuleTypes.LAMBDA_EXPRESSION) {
            var lineNo = getLineStart(node)
            val hasParentheses = node.hasParentheses()
            if (hasParentheses) append("(", lineNo)
            val it: Iterator<*> = node.parameters().iterator()
            while (it.hasNext()) {
                val v = it.next() as VariableDeclaration
                v.accept(this)
                lineNo = getLineEnd(v)
                if (it.hasNext()) {
                    append(",", lineNo)
                }
            }
            if (hasParentheses) append(")", lineNo)
            append("->", lineNo)
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: MarkerAnnotation): Boolean {
        startRule(RuleTypes.MARKER_ANNOTATION) {
            append("@", getLineStart(node))
            node.typeName.accept(this)
        }
        return false
    }

    override fun visit(node: MemberRef): Boolean {
        startRule(RuleTypes.MEMBER_REF) {
            node.qualifier?.accept(this)
            append("#", getLineStart(node.name))
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: MemberValuePair): Boolean {
        startRule(RuleTypes.MEMBER_VALUE_PAIR) {
            node.name.accept(this)
            append("=", getLineEnd(node.name))
            node.value.accept(this)
        }
        return false
    }

    override fun visit(node: MethodDeclaration): Boolean {
        startRule(RuleTypes.METHOD_DECLARATION) {
            var lineNo = getLineStart(node)
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            if (node.typeParameters().isNotEmpty()) {
                lineNo = node.typeParameters().firstOrNull()?.let { getLineStart(it as TypeParameter) }
                    ?: lineNo
                append("<", lineNo)
                val it: Iterator<*> = node.typeParameters().iterator()
                while (it.hasNext()) {
                    val t = it.next() as TypeParameter
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append(",", lineNo)
                }
                append(">", lineNo)
            }

            if (!node.isConstructor) {
                node.returnType2
                    ?.accept(this)
                    ?: append("void", lineNo)
            }
            node.name.accept(this)
            lineNo = getLineEnd(node.name)
            if (!(DOMASTUtil.isRecordDeclarationSupported(node.ast) && node.isCompactConstructor)) {
                append("(", lineNo)
                node.receiverType?.let { receiverType ->
                    receiverType.accept(this)
                    node.receiverQualifier?.let {
                        it.accept(this)
                        lineNo = getLineEnd(it)
                        append(".", lineNo)
                    }
                    append("this", lineNo)
                    if (node.parameters().isNotEmpty()) append(",", lineNo)
                }
                val it: Iterator<*> = node.parameters().iterator()
                while (it.hasNext()) {
                    val v = it.next() as SingleVariableDeclaration
                    v.accept(this)
                    lineNo = getLineEnd(v)
                    if (it.hasNext()) append(",", lineNo)
                }
                append(")", lineNo)
            }
            val size = node.extraDimensions
            val dimensions = node.extraDimensions()
            for (i in 0 until size) {
                visit(dimensions[i] as Dimension)
            }

            if (node.thrownExceptionTypes().isNotEmpty()) {
                lineNo = getLineStart(node.thrownExceptionTypes().first() as Type)
                append("throws", lineNo)
                val it: Iterator<*> = node.thrownExceptionTypes().iterator()
                while (it.hasNext()) {
                    val n = it.next() as Type
                    n.accept(this)
                    lineNo = getLineEnd(n)
                    if (it.hasNext()) {
                        append(",", lineNo)
                    }
                }
            }
            node.body?.accept(this)
                ?: append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: MethodInvocation): Boolean {
        startRule(RuleTypes.METHOD_INVOCATION) {
            var lineNo = getLineStart(node)
            node.expression?.let {
                it.accept(this)
                lineNo = getLineStart(node.name)
                append(".", lineNo)
            }
            if (node.typeArguments().isNotEmpty()) {
                append("<", lineNo)
                val it: Iterator<*> = node.typeArguments().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) {
                        append(",", lineNo)
                    }
                }
                append(">", lineNo)
            }
            node.name.accept(this)
            lineNo = getLineEnd(node.name)
            append("(", lineNo)
            val it: Iterator<*> = node.arguments().iterator()
            while (it.hasNext()) {
                val e = it.next() as Expression
                e.accept(this)
                lineNo = getLineEnd(e)
                if (it.hasNext()) {
                    append(",", lineNo)
                }
            }
            append(")", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: MethodRef): Boolean {
        startRule(RuleTypes.METHOD_REF) {
            var lineNo = getLineStart(node)
            node.qualifier?.accept(this)
            append("#", lineNo)
            node.name.accept(this)
            append("(", lineNo)
            val it: Iterator<*> = node.parameters().iterator()
            while (it.hasNext()) {
                val e = it.next() as MethodRefParameter
                e.accept(this)
                lineNo = getLineEnd(e)
                if (it.hasNext()) {
                    append(",", lineNo)
                }
            }
            append(")", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: MethodRefParameter): Boolean {
        startRule(RuleTypes.METHOD_REF_PARAMETER) {
            node.type.accept(this)
            if (node.isVarargs) append("...", getLineEnd(node.type))
            node.name?.accept(this)
        }
        return false
    }

    override fun visit(node: Modifier): Boolean {
        append(node.keyword.toString(), TokenTypes.MODIFIER, getLineStart(node))
        return false
    }

    override fun visit(node: ModuleDeclaration): Boolean {
        startRule(RuleTypes.MODULE_DECLARATION) {
            //node.javadoc?.accept(this)
            var lineNo = getLineStart(node)
            appendModifiers(node.annotations())
            if (node.isOpen) append("open", lineNo)
            append("module", lineNo)
            node.name.accept(this)
            lineNo = getLineEnd(node.name)
            append("{", lineNo)
            node.moduleStatements().forEach {
                val stmt = it as ModuleDirective
                stmt.accept(this)
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ModuleModifier): Boolean {
        append(node.keyword.toString(), TokenTypes.MODULE_MODIFIER, getLineStart(node))
        return false
    }

    private fun visit(node: ModulePackageAccess, keyword: String): Boolean {
        append(keyword, getLineStart(node))
        node.name.accept(this)
        appendTypes(node.modules(), "to", getLineEnd(node.name))
        append(";", getLineEnd(node))
        return false
    }

    override fun visit(node: NameQualifiedType): Boolean {
        startRule(RuleTypes.NAME_QUALIFIED_TYPE) {
            node.qualifier.accept(this)
            append(".", getLineEnd(node.qualifier))
            visitTypeAnnotations(node)
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: NormalAnnotation): Boolean {
        startRule(RuleTypes.NORMAL_ANNOTATION) {
            var lineNo = getLineStart(node)
            append("@", lineNo)
            node.typeName.accept(this)
            lineNo = getLineEnd(node.typeName)
            append("(", lineNo)
            val it: Iterator<*> = node.values().iterator()
            while (it.hasNext()) {
                val p = it.next() as MemberValuePair
                p.accept(this)
                lineNo = getLineEnd(p)
                if (it.hasNext()) {
                    append(",", lineNo)
                }
            }
            append(")", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: NullLiteral): Boolean {
        append("null", TokenTypes.NULL_LITERAL, getLineStart(node))
        return false
    }

    override fun visit(node: NumberLiteral): Boolean {
        append(node.token, TokenTypes.NUMBER_LITERAL, getLineStart(node))
        return false
    }

    override fun visit(node: OpensDirective): Boolean {
        return startRule(RuleTypes.OPENS_DIRECTIVE) {
            visit(node, "opens")
        }
    }

    override fun visit(node: PackageDeclaration): Boolean {
        startRule(RuleTypes.PACKAGE_DECLARATION) {
            //node.javadoc?.accept(this)
            node.annotations().forEach {
                val p = it as Annotation
                p.accept(this)
            }
            val lineNo = getLineEnd(node)
            append("package", lineNo)
            node.name.accept(this)
            append(";", lineNo)
        }
        return false
    }

    override fun visit(node: ParameterizedType): Boolean {
        startRule(RuleTypes.PARAMETERIZED_TYPE) {
            node.type.accept(this)
            var lineNo = getLineEnd(node.type)
            append("<", lineNo)
            val it: Iterator<*> = node.typeArguments().iterator()
            while (it.hasNext()) {
                val t = it.next() as Type
                t.accept(this)
                lineNo = getLineEnd(t)
                if (it.hasNext()) {
                    append(",", lineNo)
                }
            }
            append(">", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ParenthesizedExpression): Boolean {
        startRule(RuleTypes.PARENTHESIZED_EXPRESSION) {
            append("(", getLineStart(node))
            node.expression.accept(this)
            append(")", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: PostfixExpression): Boolean {
        startRule(RuleTypes.POSTFIX_EXPRESSION) {
            node.operand.accept(this)
            append(node.operator.toString(), getLineEnd(node))
        }
        return false
    }

    override fun visit(node: PrefixExpression): Boolean {
        startRule(RuleTypes.PREFIX_EXPRESSION) {
            append(node.operator.toString(), getLineStart(node))
            node.operand.accept(this)
        }
        return false
    }

    override fun visit(node: PrimitiveType): Boolean {
        startRule(RuleTypes.PRIMITIVE_TYPE) {
            visitTypeAnnotations(node)
            append(node.primitiveTypeCode.toString(), getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ProvidesDirective): Boolean {
        startRule(RuleTypes.PROVIDES_DIRECTIVE) {
            append("provides", getLineStart(node))
            node.name.accept(this)
            val endLineNo = getLineEnd(node)
            appendTypes(node.implementations(), "with", getLineEnd(node.name))
            append(";", endLineNo)
        }
        return false
    }

    override fun visit(node: ModuleQualifiedName): Boolean {
        startRule(RuleTypes.MODULE_QUALIFIED_NAME) {
            node.moduleQualifier.accept(this)
            append("/", getLineStart(node))
            val cNode: ASTNode? = node.name
            cNode?.accept(this)
        }
        return false
    }

    override fun visit(node: QualifiedName): Boolean {
        startRule(RuleTypes.QUALIFIED_NAME) {
            node.qualifier.accept(this)
            append(".", getLineStart(node))
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: QualifiedType): Boolean {
        startRule(RuleTypes.QUALIFIED_TYPE) {
            node.qualifier.accept(this)
            append(".", getLineStart(node))
            visitTypeAnnotations(node)
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: RecordDeclaration): Boolean {
        startRule(RuleTypes.RECORD_DECLARATION) {
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            append("record", getLineStart(node))
            node.name.accept(this)
            var lineNo = getLineEnd(node.name)
            if (node.typeParameters().isNotEmpty()) {
                append("<", lineNo)
                val it: Iterator<*> = node.typeParameters().iterator()
                while (it.hasNext()) {
                    val t = it.next() as TypeParameter
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append(",", lineNo)
                }
                append(">", lineNo)
            }
            append("(", lineNo)
            val it: Iterator<*> = node.recordComponents().iterator()
            while (it.hasNext()) {
                val v = it.next() as SingleVariableDeclaration
                v.accept(this)
                lineNo = getLineEnd(v)
                if (it.hasNext()) append(",", lineNo)
            }
            append(")", lineNo)
            if (node.superInterfaceTypes().isNotEmpty()) {
                append("implements", lineNo)
                val it: Iterator<*> = node.superInterfaceTypes().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append(",", lineNo)
                }
            }
            append("{", lineNo)
            node.bodyDeclarations().forEach {
                val d = it as BodyDeclaration
                d.accept(this)
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: RequiresDirective): Boolean {
        startRule(RuleTypes.REQUIRES_DIRECTIVE) {
            append("requires", getLineStart(node))
            appendModifiers(node.modifiers())
            node.name.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ReturnStatement): Boolean {
        startRule(RuleTypes.RETURN_STATEMENT) {
            append("return", getLineStart(node))
            node.expression?.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: SimpleName): Boolean {
        append(node.identifier, TokenTypes.SIMPLE_NAME, getLineStart(node))
        return false
    }

    override fun visit(node: SimpleType): Boolean {
        startRule(RuleTypes.SIMPLE_TYPE) {
            visitTypeAnnotations(node)
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: SingleMemberAnnotation): Boolean {
        startRule(RuleTypes.SINGLE_MEMBER_ANNOTATION) {
            val lineNo = getLineStart(node)
            append("@", lineNo)
            node.typeName.accept(this)
            append("(", lineNo)
            node.value.accept(this)
            append(")", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: SingleVariableDeclaration): Boolean {
        startRule(RuleTypes.SINGLE_VARIABLE_DECLARATION) {
            appendModifiers(node.modifiers())
            node.type.accept(this)
            if (node.isVarargs) {
                val annotations = node.varargsAnnotations()
                visitAnnotationsList(annotations)
                append("...", getLineStart(node))
            }
            node.name.accept(this)
            val size = node.extraDimensions
            val dimensions = node.extraDimensions()
            for (i in 0 until size) {
                visit(dimensions[i] as Dimension)
            }
            node.initializer?.let {
                append("=", getLineStart(it))
                it.accept(this)
            }
        }
        return false
    }

    override fun visit(node: StringLiteral): Boolean {
        append(node.escapedValue, TokenTypes.STRING_LITERAL, getLineStart(node))
        return false
    }

    override fun visit(node: SuperConstructorInvocation): Boolean {
        startRule(RuleTypes.SUPER_CONSTRUCTOR_INVOCATION) {
            var lineNo = getLineStart(node)
            node.expression?.let {
                it.accept(this)
                lineNo = getLineEnd(it)
                append(".", lineNo)
            }
            if (node.typeArguments().isNotEmpty()) {
                append("<", lineNo)
                val it: Iterator<*> = node.typeArguments().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append(",", lineNo)
                }
                append(">", lineNo)
            }
            append("super", lineNo)
            append("(", lineNo)
            val it: Iterator<*> = node.arguments().iterator()
            while (it.hasNext()) {
                val e = it.next() as Expression
                e.accept(this)
                lineNo = getLineEnd(e)
                if (it.hasNext()) append(",", lineNo)
            }
            append(")", lineNo)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: SuperFieldAccess): Boolean {
        startRule(RuleTypes.SUPER_FIELD_ACCESS) {
            var lineNo = getLineStart(node)
            node.qualifier?.let {
                it.accept(this)
                lineNo = getLineEnd(it)
                append(".", lineNo)
            }
            append("super", lineNo)
            append(".", lineNo)
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: SuperMethodInvocation): Boolean {
        startRule(RuleTypes.SUPER_METHOD_INVOCATION) {
            var lineNo = getLineStart(node)
            node.qualifier?.let {
                it.accept(this)
                lineNo = getLineEnd(it)
                append(".", lineNo)
            }
            append("super", lineNo)
            append(".", lineNo)
            if (node.typeArguments().isNotEmpty()) {
                append("<", lineNo)
                val it: Iterator<*> = node.typeArguments().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append(",", lineNo)
                }
                append(">", lineNo)
            }
            node.name.accept(this)
            append("(", lineNo)
            val it: Iterator<*> = node.arguments().iterator()
            while (it.hasNext()) {
                val e = it.next() as Expression
                e.accept(this)
                lineNo = getLineEnd(e)
                if (it.hasNext()) append(",", lineNo)
            }
            append(")", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: SuperMethodReference): Boolean {
        startRule(RuleTypes.SUPER_METHOD_REFERENCE) {
            var lineNo = getLineStart(node)
            node.qualifier?.let {
                it.accept(this)
                lineNo = getLineEnd(it)
                append(".", lineNo)
            }
            append("super", lineNo)
            visitReferenceTypeArguments(node.typeArguments(), lineNo, getLineStart(node.name))
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: SwitchCase): Boolean {
        startRule(RuleTypes.SWITCH_CASE) {
            if (node.isDefault) {
                val lineNo = getLineStart(node)
                append("default", lineNo)
                append(if (node.isSwitchLabeledRule) "->" else ":", lineNo)
            } else {
                var lineNo = getLineStart(node)
                append("case", lineNo)
                val it: Iterator<*> = node.expressions().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Expression
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append(",", lineNo)
                }
                append(if (node.isSwitchLabeledRule) " ->" else ":", lineNo)
            }
        }
        return false
    }

    private fun visitSwitchNode(node: ASTNode) {
        var lineNo = getLineStart(node)
        append("switch", lineNo)
        append("(", lineNo)
        when (node) {
            is SwitchExpression -> {
                node.expression.accept(this)
                lineNo = getLineEnd(node.expression)
            }
            is SwitchStatement -> {
                node.expression.accept(this)
                lineNo = getLineEnd(node.expression)
            }
        }
        append(")", lineNo)
        append("{", lineNo)
        when (node) {
            is SwitchExpression -> node.statements().forEach {
                val s = it as Statement
                s.accept(this)
            }
            is SwitchStatement -> node.statements().forEach {
                val s = it as Statement
                s.accept(this)
            }
        }
        append("}", getLineEnd(node))
    }

    override fun visit(node: SwitchExpression): Boolean {
        startRule(RuleTypes.SWITCH_EXPRESSION) {
            visitSwitchNode(node)
        }
        return false
    }

    override fun visit(node: SwitchStatement): Boolean {
        startRule(RuleTypes.SWITCH_STATEMENT) {
            visitSwitchNode(node)
        }
        return false
    }

    override fun visit(node: SynchronizedStatement): Boolean {
        startRule(RuleTypes.SYNCHRONIZED_STATEMENT) {
            val lineNo = getLineStart(node)
            append("synchronized", lineNo)
            append("(", lineNo)
            node.expression.accept(this)
            append(")", getLineStart(node.body))
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: TagElement): Boolean {
        startRule(RuleTypes.TAG_ELEMENT) {
            var lineNo = getLineStart(node)
            if (node.isNested) {
                // nested tags are always enclosed in braces
                append("{", lineNo)
            } else {
                // top-level tags always begin on a new line
                append("\n * ", lineNo)
            }
            node.tagName?.let {
                append(it, lineNo)
            }
            var previousRequiresNewLine = false
            val it: Iterator<*> = node.fragments().iterator()
            while (it.hasNext()) {
                val e = it.next() as ASTNode
                // Name, MemberRef, MethodRef, and nested TagElement do not include white space.
                // TextElements don't always include whitespace, see <https://bugs.eclipse.org/206518>.
                var currentIncludesWhiteSpace = false
                if (e is TextElement) {
                    val text = e.text
                    if (text.isNotEmpty() && ScannerHelper.isWhitespace(text[0])) {
                        currentIncludesWhiteSpace = true // workaround for https://bugs.eclipse.org/403735
                    }
                }
                if (previousRequiresNewLine && currentIncludesWhiteSpace) {
                    append("\n * ", lineNo)
                }
                previousRequiresNewLine = currentIncludesWhiteSpace
                e.accept(this)
                lineNo = getLineEnd(e)
            }
            if (node.isNested) append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: TextBlock): Boolean {
        append(node.escapedValue, TokenTypes.TEXT_BLOCK, getLineStart(node))
        return false
    }

    override fun visit(node: TextElement): Boolean {
        append(node.text, TokenTypes.TEXT_ELEMENT, getLineStart(node))
        return false
    }

    override fun visit(node: ThisExpression): Boolean {
        startRule(RuleTypes.THIS_EXPRESSION) {
            node.qualifier?.let {
                it.accept(this)
                append(".", getLineEnd(it))
            }
            append("this", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: ThrowStatement): Boolean {
        startRule(RuleTypes.THROW_STATEMENT) {
            append("throw", getLineStart(node))
            node.expression.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: TryStatement): Boolean {
        startRule(RuleTypes.TRY_STATEMENT) {
            var lineNo = getLineStart(node)
            append("try", lineNo)
            val resources = node.resources()
            if (resources.isNotEmpty()) {
                append("(", lineNo)
                val it: Iterator<*> = resources.iterator()
                while (it.hasNext()) {
                    val v = it.next() as Expression
                    v.accept(this)
                    lineNo = getLineEnd(v)
                    if (it.hasNext()) append(";", lineNo)
                }
                append(")", lineNo)
            }
            node.body.accept(this)
            node.catchClauses().forEach {
                val cc = it as CatchClause
                cc.accept(this)
            }
            node.finally?.let {
                append("finally", getLineStart(it))
                it.accept(this)
            }
        }
        return false
    }

    override fun visit(node: TypeDeclaration): Boolean {
        startRule(RuleTypes.TYPE_DECLARATION) {
            //node.javadoc?.accept(this)
            appendModifiers(node.modifiers())
            var lineNo = getLineStart(node.name)
            append(if (node.isInterface) "interface" else "class", lineNo)
            node.name.accept(this)
            if (node.typeParameters().isNotEmpty()) {
                append("<", lineNo)
                val it: Iterator<*> = node.typeParameters().iterator()
                while (it.hasNext()) {
                    val t = it.next() as TypeParameter
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append(",", lineNo)
                }
                append(">", lineNo)
            }
            node.superclassType?.let {
                append("extends", getLineStart(it))
                it.accept(this)
            }
            if (node.superInterfaceTypes().isNotEmpty()) {
                append(if (node.isInterface) "extends " else "implements ", lineNo)
                val it: Iterator<*> = node.superInterfaceTypes().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    if (it.hasNext()) {
                        append(",", lineNo)
                    }
                }
            }
            if (DOMASTUtil.isFeatureSupportedinAST(node.ast, Modifier.SEALED)) {
                if (node.permittedTypes().isNotEmpty()) {
                    append("permits", lineNo)
                    val it: Iterator<*> = node.permittedTypes().iterator()
                    while (it.hasNext()) {
                        val t = it.next() as Type
                        t.accept(this)
                        if (it.hasNext()) {
                            append(",", lineNo)
                        }
                    }
                }
            }
            append("{", lineNo)
            node.bodyDeclarations().forEach {
                val d = it as BodyDeclaration
                d.accept(this)
            }
            append("}", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: TypeDeclarationStatement): Boolean {
        startRule(RuleTypes.TYPE_DECLARATION_STATEMENT) {
            node.declaration.accept(this)
        }
        return false
    }

    override fun visit(node: TypeLiteral): Boolean {
        startRule(RuleTypes.TYPE_LITERAL) {
            node.type.accept(this)
            val lineNo = getLineEnd(node)
            append(".", lineNo)
            append("class", lineNo)
        }
        return false
    }

    override fun visit(node: TypeMethodReference): Boolean {
        startRule(RuleTypes.TYPE_METHOD_REFERENCE) {
            node.type.accept(this)
            visitReferenceTypeArguments(node.typeArguments(), getLineEnd(node.type), getLineStart(node.name))
            node.name.accept(this)
        }
        return false
    }

    override fun visit(node: TypeParameter): Boolean {
        startRule(RuleTypes.TYPE_PARAMETER) {
            appendModifiers(node.modifiers())
            node.name.accept(this)
            if (node.typeBounds().isNotEmpty()) {
                var lineNo = getLineEnd(node.name)
                append("extends", lineNo)
                val it: Iterator<*> = node.typeBounds().iterator()
                while (it.hasNext()) {
                    val t = it.next() as Type
                    t.accept(this)
                    lineNo = getLineEnd(t)
                    if (it.hasNext()) append("&", lineNo)
                }
            }
        }
        return false
    }

    override fun visit(node: UnionType): Boolean {
        startRule(RuleTypes.UNION_TYPE) {
            val it: Iterator<*> = node.types().iterator()
            while (it.hasNext()) {
                val t = it.next() as Type
                t.accept(this)
                if (it.hasNext()) append("|", getLineEnd(t))
            }
        }
        return false
    }

    override fun visit(node: UsesDirective): Boolean {
        startRule(RuleTypes.USES_DIRECTIVE) {
            append("uses", getLineStart(node))
            node.name.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: VariableDeclarationExpression): Boolean {
        startRule(RuleTypes.VARIABLE_DECLARATION_EXPRESSION) {
            appendModifiers(node.modifiers())
            node.type.accept(this)
            val it: Iterator<*> = node.fragments().iterator()
            while (it.hasNext()) {
                val f = it.next() as VariableDeclarationFragment
                f.accept(this)
                if (it.hasNext()) append(",", getLineEnd(f))
            }
        }
        return false
    }

    override fun visit(node: VariableDeclarationFragment): Boolean {
        startRule(RuleTypes.VARIABLE_DECLARATION_FRAGMENT) {
            node.name.accept(this)
            val size = node.extraDimensions
            val dimensions = node.extraDimensions()
            for (i in 0 until size) {
                visit(dimensions[i] as Dimension)
            }
            node.initializer?.let {
                append("=", getLineStart(it))
                it.accept(this)
            }
        }
        return false
    }

    override fun visit(node: VariableDeclarationStatement): Boolean {
        startRule(RuleTypes.VARIABLE_DECLARATION_STATEMENT) {
            appendModifiers(node.modifiers())
            node.type.accept(this)
            val it: Iterator<*> = node.fragments().iterator()
            while (it.hasNext()) {
                val f = it.next() as VariableDeclarationFragment
                f.accept(this)
                if (it.hasNext()) append(",", getLineEnd(f))
            }
            append(";", getLineEnd(node))
        }
        return false
    }

    override fun visit(node: WhileStatement): Boolean {
        startRule(RuleTypes.WHILE_STATEMENT) {
            val lineNo = getLineStart(node)
            append("while", lineNo)
            append("(", lineNo)
            node.expression.accept(this)
            append(")", getLineEnd(node.expression))
            node.body.accept(this)
        }
        return false
    }

    override fun visit(node: WildcardType): Boolean {
        startRule(RuleTypes.WILDCARD_TYPE) {
            visitTypeAnnotations(node)
            append("?", getLineStart(node))
            node.bound?.let {
                val text = if (node.isUpperBound) "extends" else "super"
                append(text, getLineStart(it))
                it.accept(this)
            }
        }
        return false
    }

    override fun visit(node: YieldStatement): Boolean {
        if (node.isImplicit && node.expression == null) {
            return false
        }
        startRule(RuleTypes.YIELD_STATEMENT) {
            append("yield", getLineStart(node))
            node.expression?.accept(this)
            append(";", getLineEnd(node))
        }
        return false
    }
}