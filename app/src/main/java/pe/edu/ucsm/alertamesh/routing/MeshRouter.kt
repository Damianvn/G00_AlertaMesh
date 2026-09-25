package pe.edu.ucsm.alertamesh.routing

import pe.edu.ucsm.alertamesh.model.MeshMessage
import pe.edu.ucsm.alertamesh.model.Priority
import pe.edu.ucsm.alertamesh.power.PowerMode
import pe.edu.ucsm.alertamesh.transport.Transport
import java.util.PriorityQueue
import java.util.UUID

/**
 * Enrutador mesh por inundación controlada:
 *  - Descarta mensajes ya vistos (evita bucles y duplicados).
 *  - Limita el reenvío con TTL (máximo de saltos).
 *  - Cola de prioridad: los mensajes SOS siempre salen antes que los normales.
 */
class MeshRouter(
    val nodeId: String,
    private val transport: Transport,
    private val onDeliver: (MeshMessage) -> Unit
) {
    private class Queued(val message: MeshMessage, val seq: Long)

    private val seen = object : LinkedHashMap<String, Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) =
            size > SEEN_LIMIT
    }

    private var counter = 0L
    private val outbox = PriorityQueue<Queued>(
        compareBy<Queued> { it.message.priority.ordinal }.thenBy { it.seq }
    )

    init {
        transport.setReceiver { bytes -> onReceive(bytes) }
    }

    /** Encola un mensaje propio. Se transmite en el siguiente [flush]. */
    @Synchronized
    fun send(
        payload: ByteArray,
        priority: Priority = Priority.NORMAL,
        destination: String? = null
    ): MeshMessage {
        val msg = MeshMessage(
            id = UUID.randomUUID().toString(),
            origin = nodeId,
            destination = destination,
            priority = priority,
            ttl = MeshMessage.DEFAULT_TTL,
            timestamp = System.currentTimeMillis(),
            payload = payload
        )
        seen[msg.id] = true
        enqueue(msg)
        return msg
    }

    /** Procesa bytes recibidos de un vecino. */
    @Synchronized
    fun onReceive(bytes: ByteArray) {
        val msg = try {
            MeshMessage.fromBytes(bytes)
        } catch (e: Exception) {
            return // mensaje malformado: se descarta
        }
        if (seen.containsKey(msg.id)) return
        seen[msg.id] = true

        if (msg.destination == null || msg.destination == nodeId) onDeliver(msg)
        if (msg.destination != nodeId && msg.ttl > 1) enqueue(msg.copy(ttl = msg.ttl - 1))
    }

    /** Transmite hasta [PowerMode.maxPerFlush] mensajes, SOS primero. Devuelve cuántos envió. */
    @Synchronized
    fun flush(mode: PowerMode = PowerMode.NORMAL): Int {
        if (!transport.hasPeers) return 0 // sin vecinos: los mensajes esperan en cola
        var sent = 0
        while (sent < mode.maxPerFlush) {
            val next = outbox.poll() ?: break
            transport.broadcast(next.message.toBytes())
            sent++
        }
        return sent
    }

    @Synchronized
    fun pending(): Int = outbox.size

    private fun enqueue(msg: MeshMessage) {
        outbox.add(Queued(msg, counter++))
    }

    private companion object {
        const val SEEN_LIMIT = 2000
    }
}
