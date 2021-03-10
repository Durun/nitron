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
import kotlin.experimental.and

fun ByteArray.toMD5(): MD5 = MD5.of(this)
fun Collection<Byte>.toMD5(): MD5 = MD5.of(this)
fun Blob.toMD5(): MD5 = this.getBytes(1, MD5.length).toMD5()

fun MD5.toBlob(): Blob = SerialBlob(this.toByteArray())
fun ByteArray.toBlob(): Blob = SerialBlob(this)

fun Blob.toByteArray(): ByteArray = this.getBytes(1, length().toInt())


private const val LO_MASK: Byte = 0x0F
private const val HI_MASK: Byte = 0xF0.toByte()

@Serializable(with = MD5.Serializer::class)
class MD5 private constructor(
    val bytes: ByteArray
) : List<Byte> by bytes.asList() {
    companion object {
        const val length: Int = 16
        private val md5 = ThreadLocal.withInitial { MessageDigest.getInstance("MD5") }

        fun of(bytes: ByteArray): MD5 = MD5(bytes.clone())
        fun of(bytes: Collection<Byte>): MD5 = MD5(bytes.toByteArray())

        @JvmStatic
        fun digest(input: String): MD5 {
            return md5.get().digest(input.toByteArray(Charsets.UTF_8)).toMD5()
        }
    }

    init {
        require(bytes.size == length)
    }

    override fun toString(): String {
        return bytes.joinToString("") { it.toHexString() }
    }

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

private fun lowerBitsToChar(i: Int): Char {
    return when (i and 0xF) {
        0x0 -> '0'
        0x1 -> '1'
        0x2 -> '2'
        0x3 -> '3'
        0x4 -> '4'
        0x5 -> '5'
        0x6 -> '6'
        0x7 -> '7'
        0x8 -> '8'
        0x9 -> '9'
        0xA -> 'a'
        0xB -> 'b'
        0xC -> 'c'
        0xD -> 'd'
        0xE -> 'e'
        0xF -> 'f'
        else -> throw IllegalStateException("Can't convert $i to hex expression")
    }
}

private fun Byte.toHexString(): String {
    val lo = (this and LO_MASK).toInt()         // 0x0F
    val hi = (this and HI_MASK).toInt() / 0x10  // 0xF0
    val loChar = lowerBitsToChar(lo)
    val hiChar = lowerBitsToChar(hi)
    return "$hiChar$loChar"
}