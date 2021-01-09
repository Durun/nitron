package io.github.durun.nitron.core.ast.node

import io.github.durun.nitron.core.MD5


fun MD5.Companion.digest(ast: AstNode): MD5 = digest(ast.getText())
fun MD5.Companion.digest(asts: List<AstNode>): MD5 = digest(asts.joinToString(" ") { it.getText() })