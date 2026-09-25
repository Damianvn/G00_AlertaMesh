package pe.edu.ucsm.alertamesh.transport

/**
 * Enlace directo con los vecinos.
 * Implementación real: [NearbyTransport] (Bluetooth / Wi-Fi Direct vía Nearby Connections).
 * Implementación de pruebas: [SimulatedNetwork].
 */
interface Transport {
    /** false = no hay vecinos conectados; el router conserva los mensajes en cola. */
    val hasPeers: Boolean get() = true

    /** Envía los bytes a todos los vecinos conectados. */
    fun broadcast(data: ByteArray)

    /** Registra quién recibe los bytes que llegan de vecinos. */
    fun setReceiver(receiver: (ByteArray) -> Unit)
}
