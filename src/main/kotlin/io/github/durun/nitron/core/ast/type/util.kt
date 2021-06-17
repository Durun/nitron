package io.github.durun.nitron.core.ast.type


fun createNodeTypePool(grammarName: String, tokenTypes: List<String>, ruleTypes: List<String>): NodeTypePool {
    return NodeTypePool.of(
            grammarName,
            tokenTypes = tokenTypes.mapIndexed {index, name -> TokenType(index, name) },
            ruleTypes = ruleTypes.mapIndexed { index, name -> RuleType(index, name) }
    )
}