package io.github.durun.nitron.core

import io.github.durun.nitron.core.ast.node.AstNode
import java.security.MessageDigest
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

fun ByteArray.toMD5(): MD5 = MD5(this)
fun Blob.toMD5(): MD5 = this.getBytes(1, MD5.length).toMD5()
fun AstNode.toMD5(): MD5 = MD5.digest(this.getText())

fun MD5.toBlob(): Blob = SerialBlob(this.bytes)

class MD5 internal constructor(val bytes: ByteArray) : List<Byte> by bytes.asList() {
	companion object {
		const val length: Int = 16
		private val md5 = MessageDigest.getInstance("MD5")

		fun digest(input: String): MD5 {
			return md5.digest(input.toByteArray(Charsets.UTF_8)).toMD5()
		}
	}

	init {
		require(bytes.size == length)
	}

	override fun toString(): String = String.format(
			"%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
			bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
			bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MD5

		if (!bytes.contentEquals(other.bytes)) return false

		return true
	}

	override fun hashCode(): Int = bytes.contentHashCode()
}
