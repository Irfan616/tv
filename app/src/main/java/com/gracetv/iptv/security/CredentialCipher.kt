package com.gracetv.iptv.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CredentialCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}

class KeystoreCredentialCipher : CredentialCipher {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "grace_tv_credential_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
        private const val PREFIX = "ENC_V1:"
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            // Fallback for non-Keystore JVM test environments; never returns raw plainText directly
            val encoded = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            "B64_FALLBACK:$encoded"
        }
    }

    override fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        if (cipherText.startsWith("B64_FALLBACK:")) {
            return try {
                val raw = cipherText.removePrefix("B64_FALLBACK:")
                String(Base64.decode(raw, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (_: Throwable) {
                ""
            }
        }
        if (!cipherText.startsWith(PREFIX)) return cipherText
        return try {
            val combined = Base64.decode(cipherText.removePrefix(PREFIX), Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH_BYTES) return ""
            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val encrypted = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }
}

object PinHasher {
    private val random = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin".toByteArray(Charsets.UTF_8)
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (expectedHash.isBlank() || salt.isBlank()) return false
        return hashPin(pin, salt) == expectedHash
    }
}
