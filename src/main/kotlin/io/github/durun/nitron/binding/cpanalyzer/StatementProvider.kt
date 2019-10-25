package io.github.durun.nitron.binding.cpanalyzer

import java.nio.file.Paths

object StatementProvider {
    private val processors: MutableMap<String, CodeProcessor> = mutableMapOf()

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
            Statement(
                    listOf(Token(statement.getText() ?: "", statement.range?.line?.start ?: 0, 1)), // TODO
                    rText = statement.getText() ?: throw Exception(),
                    nText = nText
            )
        }
    }
}