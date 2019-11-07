package io.github.durun.nitron.binding.cpanalyzer

import io.github.durun.nitron.core.ast.visitor.AstFlattenVisitor
import java.nio.file.Paths

object StatementProvider {
    @JvmStatic
    private val processors: MutableMap<String, CodeProcessor> = mutableMapOf()

    @JvmStatic
    private fun getProcessor(lang: String): CodeProcessor {
        return processors[lang]
                ?: let {
                    val processor = CodeProcessor(Paths.get("testdata/$lang.json"))
                    processors[lang] = processor
                    processor
                }
    }

    @JvmStatic
    fun parse(fileText: String, lang: String): List<Statement> {
        val processor = getProcessor(lang)
        val result = processor.process(fileText)
        return result.map { (statement, nText) ->
            val tokens = statement.accept(AstFlattenVisitor)
                    .mapIndexed { index, it ->
                        Token(
                                value = it.token,
                                line = it.range.line.start,
                                index = index
                        )
                    }
            Statement(
                    tokens = tokens,
                    rText = statement.getText() ?: throw Exception(),
                    nText = nText
            )
        }
    }
}