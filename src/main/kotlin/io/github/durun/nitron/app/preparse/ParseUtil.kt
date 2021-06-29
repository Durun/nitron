package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.AstSerializers
import io.github.durun.nitron.core.config.LangConfig
import io.github.durun.nitron.core.config.NitronConfig
import io.github.durun.nitron.core.parser.AstBuilder
import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater

class ParseUtil(
    val config: NitronConfig
) {
    private val astBuilders: MutableMap<String, AstBuilder> = mutableMapOf()
    private val encoder = AstSerializers.encodeOnlyJson

    fun parseText(text: String, langName: String, langConfig: LangConfig): String {
        val astBuilder = synchronized(astBuilders) {
            astBuilders.computeIfAbsent(langName) { langConfig.grammar.getParser() }
        }
        val ast = astBuilder.parse(text.reader())
        return encoder.encodeToString(ast)
    }
}

fun ByteArray.deflate(): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION, true)

    deflater.setInput(this)
    deflater.finish()

    val buffer = ByteArray(this.size + 1)
    deflater.deflate(buffer)

    return buffer.sliceArray(0..deflater.bytesRead.toInt())
        .also { deflater.end() }
}

fun ByteArray.inflate(): ByteArray {
    val inflater = Inflater(true)

    inflater.setInput(this)

    val buffer = ByteArray(Short.MAX_VALUE.toInt())
    val length = inflater.inflate(buffer)

    return buffer.sliceArray(0 until length)
        .also { inflater.end() }
}

fun String.gzip(): ByteArray {
    val stream = ByteArrayOutputStream()
    GZIPOutputStream(stream).bufferedWriter().use {
        it.write(this)
    }
    return stream.toByteArray()
}

fun ByteArray.ungzip(): String {
    return GZIPInputStream(this.inputStream()).bufferedReader().use {
        it.readText()
    }
}