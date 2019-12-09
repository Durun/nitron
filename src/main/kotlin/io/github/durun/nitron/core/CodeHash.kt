package io.github.durun.nitron.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.durun.nitron.core.ast.node.AstNode
import java.security.MessageDigest
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

private val mapper = jacksonObjectMapper()
private const val hashLength = 16

internal fun ByteArray.toBlob(): Blob {
    assert(this.size == hashLength)
    return SerialBlob(this)
}

internal fun Blob.toBytes(): ByteArray {
    assert(this.length() == hashLength.toLong())
    return this.getBytes(1, hashLength)
}

private val md5: MessageDigest = MessageDigest.getInstance("MD5")
fun codeHashOf(code: String): ByteArray {
    return md5.digest(code.toByteArray(Charsets.UTF_8))
}

internal fun AstNode.toHash(): ByteArray {
    return codeHashOf(this.getText())
}

internal fun encodeByteArray(bytes: ByteArray): String {
    return bytes.joinToString("") { String.format("%02x", it) }
}

internal fun decodeByteArray(str: String): ByteArray {
    return str.chunked(2)
            .map { Integer.decode("0x$it") }
            .map { it.toByte() }
            .toByteArray()
}
