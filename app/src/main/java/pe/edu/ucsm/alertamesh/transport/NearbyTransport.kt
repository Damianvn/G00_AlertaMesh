package pe.edu.ucsm.alertamesh.transport

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

/**
 * Transporte real entre teléfonos Android. Nearby Connections usa Bluetooth LE para descubrir
 * y Bluetooth / Wi-Fi Direct / Wi-Fi LAN para transferir, sin Internet ni red celular.
 * Estrategia P2P_CLUSTER: cada teléfono puede tener varios vecinos a la vez (malla).
 */
class NearbyTransport(
    context: Context,
    private val nodeId: String,
    private val onPeers: (Int) -> Unit,
    private val onError: (String) -> Unit
) : Transport {

    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val handler = Handler(Looper.getMainLooper())

    private val connected = mutableSetOf<String>()
    private val pending = mutableSetOf<String>()
    private val known = mutableMapOf<String, String>() // endpointId -> nodeId remoto
    private var receiver: ((ByteArray) -> Unit)? = null

    override val hasPeers: Boolean get() = connected.isNotEmpty()

    override fun setReceiver(receiver: (ByteArray) -> Unit) {
        this.receiver = receiver
    }

    override fun broadcast(data: ByteArray) {
        if (connected.isEmpty()) return
        client.sendPayload(connected.toList(), Payload.fromBytes(data))
            .addOnFailureListener { onError("Error al enviar: ${it.message}") }
    }

    fun start() {
        val advertising = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        val discovery = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(nodeId, SERVICE_ID, connectionCallback, advertising)
            .addOnFailureListener { onError("No se pudo anunciar: ${it.message}") }
        client.startDiscovery(SERVICE_ID, discoveryCallback, discovery)
            .addOnFailureListener { onError("No se pudo buscar: ${it.message}") }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connected.clear()
        pending.clear()
        known.clear()
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { receiver?.invoke(it) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pending.remove(endpointId)
            if (result.status.isSuccess) {
                connected.add(endpointId)
                onPeers(connected.size)
            } else {
                scheduleRequest(endpointId, RETRY_MS)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connected.remove(endpointId)
            onPeers(connected.size)
            scheduleRequest(endpointId, RETRY_MS)
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val remoteId = info.endpointName
            if (remoteId == nodeId) return
            known[endpointId] = remoteId
            // El de id menor inicia la conexión; el otro espera unos segundos por si falla.
            scheduleRequest(endpointId, if (nodeId < remoteId) 0L else FALLBACK_MS)
        }

        override fun onEndpointLost(endpointId: String) {
            known.remove(endpointId)
        }
    }

    private fun scheduleRequest(endpointId: String, delayMs: Long) {
        handler.postDelayed({ requestIfNeeded(endpointId) }, delayMs)
    }

    private fun requestIfNeeded(endpointId: String) {
        if (endpointId !in known || endpointId in connected || endpointId in pending) return
        pending.add(endpointId)
        client.requestConnection(nodeId, endpointId, connectionCallback)
            .addOnFailureListener {
                pending.remove(endpointId)
                scheduleRequest(endpointId, RETRY_MS)
            }
    }

    private companion object {
        const val SERVICE_ID = "pe.edu.ucsm.alertamesh"
        val STRATEGY: Strategy = Strategy.P2P_CLUSTER
        const val FALLBACK_MS = 3_000L
        const val RETRY_MS = 8_000L
    }
}
