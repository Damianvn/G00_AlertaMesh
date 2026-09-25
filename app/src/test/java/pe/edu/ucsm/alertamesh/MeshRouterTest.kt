package pe.edu.ucsm.alertamesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import pe.edu.ucsm.alertamesh.model.MeshMessage
import pe.edu.ucsm.alertamesh.model.Priority
import pe.edu.ucsm.alertamesh.power.PowerMode
import pe.edu.ucsm.alertamesh.routing.MeshRouter
import pe.edu.ucsm.alertamesh.transport.SimulatedNetwork
import pe.edu.ucsm.alertamesh.transport.Transport
import org.junit.Test

class MeshRouterTest {

    private class PipeTransport : Transport {
        val sent = mutableListOf<ByteArray>()
        var peers = true
        override val hasPeers: Boolean get() = peers
        var receiver: ((ByteArray) -> Unit)? = null
        override fun broadcast(data: ByteArray) { sent.add(data) }
        override fun setReceiver(receiver: (ByteArray) -> Unit) { this.receiver = receiver }
    }

    private fun message(ttl: Int, dest: String? = null) = MeshMessage(
        id = "m-1", origin = "X", destination = dest, priority = Priority.NORMAL,
        ttl = ttl, timestamp = 0L, payload = "hola".toByteArray()
    )

    @Test
    fun sosSaleAntesQueNormal() {
        val t = PipeTransport()
        val r = MeshRouter("A", t) {}
        r.send("n1".toByteArray())
        r.send("n2".toByteArray())
        r.send("sos".toByteArray(), Priority.SOS)
        r.flush()
        assertEquals(Priority.SOS, MeshMessage.fromBytes(t.sent[0]).priority)
        assertEquals(3, t.sent.size)
    }

    @Test
    fun mensajeLlegaPorMultisalto() {
        val net = SimulatedNetwork()
        net.link("A", "B"); net.link("B", "C")
        val delivered = mutableListOf<MeshMessage>()
        val a = MeshRouter("A", net.transportFor("A")) {}
        val b = MeshRouter("B", net.transportFor("B")) {}
        MeshRouter("C", net.transportFor("C")) { delivered.add(it) }.let { c ->
            a.send("auxilio".toByteArray(), Priority.SOS)
            repeat(5) { listOf(a, b, c).forEach { r -> r.flush() } }
        }
        assertEquals(1, delivered.size)
        assertEquals(2, delivered[0].hops)
    }

    @Test
    fun duplicadosSeDescartan() {
        val t = PipeTransport()
        var count = 0
        MeshRouter("B", t) { count++ }
        val bytes = message(ttl = 5).toBytes()
        t.receiver!!.invoke(bytes)
        t.receiver!!.invoke(bytes)
        assertEquals(1, count)
    }

    @Test
    fun ttlAgotadoNoSeReenvia() {
        val t = PipeTransport()
        var count = 0
        val r = MeshRouter("B", t) { count++ }
        t.receiver!!.invoke(message(ttl = 1).toBytes())
        assertEquals(1, count)
        assertEquals(0, r.pending())
    }

    @Test
    fun mensajeMalformadoSeIgnora() {
        val t = PipeTransport()
        var count = 0
        MeshRouter("B", t) { count++ }
        t.receiver!!.invoke(byteArrayOf(1, 2, 3))
        assertEquals(0, count)
    }

    @Test
    fun modoBajoConsumoLimitaEnvioPeroSosVaPrimero() {
        val t = PipeTransport()
        val r = MeshRouter("A", t) {}
        repeat(15) { r.send("n$it".toByteArray()) }
        r.send("sos".toByteArray(), Priority.SOS)
        val sent = r.flush(PowerMode.LOW_POWER)
        assertEquals(PowerMode.LOW_POWER.maxPerFlush, sent)
        assertEquals(Priority.SOS, MeshMessage.fromBytes(t.sent[0]).priority)
        assertTrue(r.pending() > 0)
    }

    @Test
    fun sinVecinosLosMensajesEsperanEnCola() {
        val t = PipeTransport()
        t.peers = false
        val r = MeshRouter("A", t) {}
        r.send("hola".toByteArray())
        assertEquals(0, r.flush())
        assertEquals(1, r.pending())
        t.peers = true
        assertEquals(1, r.flush())
        assertEquals(0, r.pending())
    }
}
