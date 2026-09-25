package pe.edu.ucsm.alertamesh.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Identidad local del usuario. [nodeId] es aleatorio: es lo único que se anuncia por radio. */
data class Profile(val name: String, val dni: String, val nodeId: String)

/** Contenido de un mensaje. Se cifra completo antes de entrar a la malla (el DNI nunca viaja en claro). */
data class ChatPacket(val name: String, val dni: String, val text: String) {
    fun toBytes(): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeUTF(name)
            out.writeUTF(dni)
            out.writeUTF(text)
        }
        return bos.toByteArray()
    }

    companion object {
        fun fromBytes(data: ByteArray): ChatPacket {
            DataInputStream(ByteArrayInputStream(data)).use { inp ->
                return ChatPacket(inp.readUTF(), inp.readUTF(), inp.readUTF())
            }
        }
    }
}

/** Mensaje ya listo para mostrar en pantalla. */
data class ChatMessage(
    val senderName: String,
    val senderDni: String,
    val text: String,
    val sos: Boolean,
    val mine: Boolean,
    val hops: Int
)
