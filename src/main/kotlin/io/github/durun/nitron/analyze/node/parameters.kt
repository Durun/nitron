package io.github.durun.nitron.analyze.node

import io.github.durun.nitron.analyze.AnalyzeContext
import io.github.durun.nitron.analyze.contexts.ifPattern
import io.github.durun.nitron.analyze.contexts.typeWithParameters
import io.github.durun.nitron.analyze.query.AnalyzeQuery
import io.github.durun.nitron.inout.model.ast.SerializableAst


fun AnalyzeContext.ifInherit(to: String, types: Array<String>, requireInherited: Array<Boolean> = emptyArray()): AnalyzeQuery<Boolean> {
    return this.ifPattern {
        val beforeText = it.node.first.text
        val afterTokenLists = it.node.second.typeWithParameters(type = typeOf(to), parameterTypes = typesOf(*types))
        afterTokenLists.filter { list ->
            list.zip(requireInherited)
                    .all { (token, require) ->
                        val tokenText = token.let{
                            when (true) {
                                it is SerializableAst.NormalizedRuleNode -> it.text.removeNumbers()
                                else -> it.text
                            }
                        }
                        !require || (require && beforeText.contains(tokenText))
                    }
        }.count() > 0
    }
}

private fun String.removeNumbers() = this.replace("[0-9]+", "")