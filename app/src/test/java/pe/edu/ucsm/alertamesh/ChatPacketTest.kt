package pe.edu.ucsm.alertamesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.edu.ucsm.alertamesh.crypto.CryptoManager
import pe.edu.ucsm.alertamesh.model.ChatPacket
import java.security.GeneralSecurityException

class ChatPacketTest {

    @Test
    fun serializaYDeserializa() {
        val p = ChatPacket("Ana Pérez", "12345678", "Estoy atrapada en el 2do piso")
        assertEquals(p, ChatPacket.fromBytes(p.toBytes()))
    }

    @Test
    fun mismoCodigoDeRedDescifraYOtroNo() {
        val crypto = CryptoManager()
        val k1 = CryptoManager.keyFromPassphrase("alertamesh")
        val k2 = CryptoManager.keyFromPassphrase("alertamesh")
        val otra = CryptoManager.keyFromPassphrase("otra-red")
        val plain = ChatPacket("Ana", "12345678", "hola").toBytes()
        val enc = crypto.encrypt(k1, plain)
        assertEquals(ChatPacket.fromBytes(plain), ChatPacket.fromBytes(crypto.decrypt(k2, enc)))
        assertThrows(GeneralSecurityException::class.java) { crypto.decrypt(otra, enc) }
    }
}
