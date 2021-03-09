package io.github.durun.nitron.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.security.MessageDigest
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

fun ByteArray.toMD5(): MD5 = MD5.of(this)
fun Collection<Byte>.toMD5(): MD5 = MD5.of(this)
fun Blob.toMD5(): MD5 = this.getBytes(1, MD5.length).toMD5()

fun MD5.toBlob(): Blob = SerialBlob(this.toByteArray())
fun ByteArray.toBlob(): Blob = SerialBlob(this)

fun Blob.toByteArray(): ByteArray = this.getBytes(1, length().toInt())

@Serializable(with = MD5.Serializer::class)
class MD5 private constructor(
	val bytes: ByteArray
) : List<Byte> by bytes.asList() {
	companion object {
		const val length: Int = 16
		private val md5 = MessageDigest.getInstance("MD5")

		fun of(bytes: ByteArray): MD5 = MD5(bytes.clone())
		fun of(bytes: Collection<Byte>): MD5 = MD5(bytes.toByteArray())

		@JvmStatic
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

	object Serializer : KSerializer<MD5> {
		override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MD5", PrimitiveKind.STRING)
		override fun serialize(encoder: Encoder, value: MD5) {
			encoder.encodeString(value.toString())
		}

		override fun deserialize(decoder: Decoder): MD5 {
			val bytes = decoder.decodeString()
					.chunked(2)
					.map { Integer.decode("0x$it").toByte() }
			return bytes.toMD5()
		}
	}
}