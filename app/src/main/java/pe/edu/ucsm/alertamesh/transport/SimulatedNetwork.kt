package pe.edu.ucsm.alertamesh.transport

/** Red simulada en memoria: un nodo solo alcanza a los nodos con los que tiene enlace. */
class SimulatedNetwork {
    private val links = mutableMapOf<String, MutableSet<String>>()
    private val receivers = mutableMapOf<String, (ByteArray) -> Unit>()

    fun link(a: String, b: String) {
        links.getOrPut(a) { mutableSetOf() }.add(b)
        links.getOrPut(b) { mutableSetOf() }.add(a)
    }

    fun transportFor(nodeId: String): Transport = object : Transport {
        override fun broadcast(data: ByteArray) {
            links[nodeId]?.forEach { neighbor -> receivers[neighbor]?.invoke(data) }
        }

        override fun setReceiver(receiver: (ByteArray) -> Unit) {
            receivers[nodeId] = receiver
        }
    }
}
