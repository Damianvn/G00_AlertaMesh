package pe.edu.ucsm.alertamesh.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Prioridad del mensaje. SOS se transmite siempre antes que NORMAL. */
enum class Priority { SOS, NORMAL }

/**
 * Mensaje que viaja por la malla.
 * [destination] = null significa difusión a todos los nodos.
 * [payload] es opaco para el router (puede venir cifrado con CryptoManager).
 */
data class MeshMessage(
    val id: String,
    val origin: String,
    val destination: String?,
    val priority: Priority,
    val ttl: Int,
    val timestamp: Long,
    val payload: ByteArray
) {
    /** Número de enlaces recorridos hasta ahora (1 = llegó directo del origen). */
    val hops: Int get() = DEFAULT_TTL - ttl + 1

    fun toBytes(): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeUTF(id)
            out.writeUTF(origin)
            out.writeUTF(destination ?: "")
            out.writeByte(priority.ordinal)
            out.writeByte(ttl)
            out.writeLong(timestamp)
            out.writeInt(payload.size)
            out.write(payload)
        }
        return bos.toByteArray()
    }

    companion object {
        const val DEFAULT_TTL = 7
        private const val MAX_PAYLOAD = 4096

        /** Lanza excepción si los bytes están malformados. */
        fun fromBytes(data: ByteArray): MeshMessage {
            DataInputStream(ByteArrayInputStream(data)).use { inp ->
                val id = inp.readUTF()
                val origin = inp.readUTF()
                val dest = inp.readUTF().ifEmpty { null }
                val priority = Priority.values()[inp.readUnsignedByte()]
                val ttl = inp.readUnsignedByte()
                val timestamp = inp.readLong()
                val size = inp.readInt()
                require(size in 0..MAX_PAYLOAD) { "Tamaño de payload inválido" }
                val payload = ByteArray(size)
                inp.readFully(payload)
                return MeshMessage(id, origin, dest, priority, ttl, timestamp, payload)
            }
        }
    }
}
