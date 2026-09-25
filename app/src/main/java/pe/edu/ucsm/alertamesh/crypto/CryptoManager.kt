package pe.edu.ucsm.alertamesh.crypto

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado extremo a extremo:
 *  - Intercambio de clave: ECDH sobre secp256r1.
 *  - Cifrado de datos: AES-256-GCM (confidencialidad + integridad).
 *  - Autenticación de claves: huella (fingerprint) SHA-256 de la clave pública,
 *    que los usuarios comparan por otro medio (voz/QR) para evitar suplantación.
 */
class CryptoManager {
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private val random = SecureRandom()

    val publicKeyBytes: ByteArray get() = keyPair.public.encoded

    /** Huella legible de una clave pública, ej. "A1B2-C3D4-E5F6-0718". */
    fun fingerprint(publicKey: ByteArray = publicKeyBytes): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(publicKey)
        return hash.take(8).joinToString("") { "%02X".format(it) }.chunked(4).joinToString("-")
    }

    fun deriveSessionKey(peerPublicKey: ByteArray): SecretKey {
        val peer = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(peerPublicKey))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(keyPair.private)
        agreement.doPhase(peer, true)
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(agreement.generateSecret())
        return SecretKeySpec(keyBytes, "AES")
    }

    /** Devuelve IV (12 bytes) + texto cifrado + etiqueta GCM. */
    fun encrypt(key: SecretKey, plain: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        val iv = ByteArray(IV_SIZE).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad)
        return iv + cipher.doFinal(plain)
    }

    /** Lanza GeneralSecurityException si el mensaje fue alterado o la clave es incorrecta. */
    fun decrypt(key: SecretKey, data: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        require(data.size > IV_SIZE) { "Datos cifrados inválidos" }
        val iv = data.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE)
    }

    companion object {
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
        private val SALT = "AlertaMesh-v1".toByteArray()

        /** Deriva la clave AES-256 del canal a partir del código de red compartido. */
        fun keyFromPassphrase(passphrase: String): SecretKey {
            val spec = PBEKeySpec(passphrase.toCharArray(), SALT, 60_000, 256)
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        }
    }
}
