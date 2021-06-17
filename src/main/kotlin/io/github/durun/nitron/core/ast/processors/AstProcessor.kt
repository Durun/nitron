package io.github.durun.nitron.core.ast.processors

import io.github.durun.nitron.core.ast.node.AstNode

interface AstProcessor<R> {
	fun process(ast: AstNode): R
}