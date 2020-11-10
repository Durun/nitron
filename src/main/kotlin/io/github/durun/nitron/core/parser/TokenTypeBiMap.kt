package io.github.durun.nitron.core.parser

import org.antlr.v4.runtime.Parser

class TokenTypeBiMap(parser: Parser) {
    val fromName: Map<String, Int> = parser.tokenTypeMap
    val fromIndex: Map<Int, String>

    init {
        /* remove overlaps
             Strategy: Do not prioritize the name not having single quotation (')
             ex) { "'None'": 28, "NONE": 28 } -> { "NONE": 28 }
        * */
        fromIndex = fromName.entries.groupBy { it.value }
                .map { (index, entries) ->
                    val names = entries.map { it.key }
                    val name = names.firstOrNull { !it.contains('\'') } // try to remove the name not having single quotation (')
                            ?: names.first()
                    index to name
                }.toMap()
        assert(fromIndex.keys.size == fromIndex.values.size)
    }
}
