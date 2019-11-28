package io.github.durun.nitron.core.parser

import org.antlr.v4.runtime.Parser

class TokenTypeBiMap(parser: Parser) {
    val fromName: Map<String, Int> = parser.tokenTypeMap
    val fromIndex: Map<Int, String>

    init {
        fromIndex = fromName
                .filterNot { it.key.contains('\'') }
                .entries
                .map { it.value to it.key }
                .toMap()
        assert(fromIndex.keys.size == fromIndex.values.size)
    }
}