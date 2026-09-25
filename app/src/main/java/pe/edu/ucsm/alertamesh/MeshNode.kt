package pe.edu.ucsm.alertamesh

import android.content.Context
import android.os.Handler
import android.os.Looper
import pe.edu.ucsm.alertamesh.crypto.CryptoManager
import pe.edu.ucsm.alertamesh.model.ChatMessage
import pe.edu.ucsm.alertamesh.model.ChatPacket
import pe.edu.ucsm.alertamesh.model.MeshMessage
import pe.edu.ucsm.alertamesh.model.Priority
import pe.edu.ucsm.alertamesh.model.Profile
import pe.edu.ucsm.alertamesh.power.PowerMode
import pe.edu.ucsm.alertamesh.routing.MeshRouter
import pe.edu.ucsm.alertamesh.transport.NearbyTransport

/** Une transporte real + enrutador mesh + cifrado. Todo corre en el hilo principal. */
class MeshNode(
    context: Context,
    private val profile: Profile,
    networkCode: String,
    private val listener: Listener
) {
    interface Listener {
        fun onChat(msg: ChatMessage)
        fun onPeers(count: Int)
        fun onStatus(text: String)
    }

    var powerMode: PowerMode = PowerMode.NORMAL

    private val crypto = CryptoManager()
    private val key = CryptoManager.keyFromPassphrase(networkCode)
    private val transport = NearbyTransport(
        context, profile.nodeId,
        onPeers = { listener.onPeers(it) },
        onError = { listener.onStatus(it) }
    )
    private val router = MeshRouter(profile.nodeId, transport) { deliver(it) }
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val flushLoop = object : Runnable {
        override fun run() {
            if (!running) return
            router.flush(powerMode)
            handler.postDelayed(this, powerMode.flushIntervalMs)
        }
    }

    fun start() {
        running = true
        transport.start()
        handler.post(flushLoop)
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        transport.stop()
    }

    fun sendText(text: String, sos: Boolean) {
        val plain = ChatPacket(profile.name, profile.dni, text).toBytes()
        val priority = if (sos) Priority.SOS else Priority.NORMAL
        router.send(crypto.encrypt(key, plain), priority)
        listener.onChat(ChatMessage(profile.name, profile.dni, text, sos, mine = true, hops = 0))
        if (sos) router.flush(powerMode) // los SOS no esperan al ciclo de envío
    }

    private fun deliver(m: MeshMessage) {
        try {
            val packet = ChatPacket.fromBytes(crypto.decrypt(key, m.payload))
            listener.onChat(
                ChatMessage(packet.name, packet.dni, packet.text, m.priority == Priority.SOS, false, m.hops)
            )
        } catch (e: Exception) {
            // Otro código de red o mensaje alterado: no se muestra (igual se reenvía a la malla).
            listener.onStatus("Llegó un mensaje que no se pudo leer: revisa que todos usen el mismo código de red.")
        }
    }
}
