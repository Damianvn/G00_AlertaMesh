package pe.edu.ucsm.alertamesh

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.edu.ucsm.alertamesh.crypto.CryptoManager
import java.security.GeneralSecurityException

class CryptoManagerTest {

    @Test
    fun ambosExtremosDerivanLaMismaClave() {
        val alice = CryptoManager()
        val bob = CryptoManager()
        val k1 = alice.deriveSessionKey(bob.publicKeyBytes)
        val k2 = bob.deriveSessionKey(alice.publicKeyBytes)
        assertArrayEquals(k1.encoded, k2.encoded)
    }

    @Test
    fun cifraYDescifra() {
        val alice = CryptoManager()
        val bob = CryptoManager()
        val plain = "Estoy atrapado, calle Mercaderes 123".toByteArray()
        val enc = alice.encrypt(alice.deriveSessionKey(bob.publicKeyBytes), plain)
        val dec = bob.decrypt(bob.deriveSessionKey(alice.publicKeyBytes), enc)
        assertArrayEquals(plain, dec)
    }

    @Test
    fun mensajeAlteradoEsRechazado() {
        val alice = CryptoManager()
        val bob = CryptoManager()
        val key = alice.deriveSessionKey(bob.publicKeyBytes)
        val enc = alice.encrypt(key, "hola".toByteArray())
        enc[enc.size - 1] = (enc[enc.size - 1].toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) {
            bob.decrypt(bob.deriveSessionKey(alice.publicKeyBytes), enc)
        }
    }

    @Test
    fun terceroNoPuedeDescifrar() {
        val alice = CryptoManager()
        val bob = CryptoManager()
        val eve = CryptoManager()
        val enc = alice.encrypt(alice.deriveSessionKey(bob.publicKeyBytes), "secreto".toByteArray())
        assertThrows(GeneralSecurityException::class.java) {
            eve.decrypt(eve.deriveSessionKey(alice.publicKeyBytes), enc)
        }
    }

    @Test
    fun huellasDistintasParaClavesDistintas() {
        assertNotEquals(CryptoManager().fingerprint(), CryptoManager().fingerprint())
        val c = CryptoManager()
        assertEquals(c.fingerprint(), c.fingerprint())
    }
}
