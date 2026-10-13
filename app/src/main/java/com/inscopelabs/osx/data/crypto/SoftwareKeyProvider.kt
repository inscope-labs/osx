package com.inscopelabs.osx.data.crypto

import com.inscopelabs.osx.domain.profile.OperationalProfile
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Software-backed cryptographic provider for BASIC Operational Profile.
 */
class SoftwareKeyProvider : CryptoProvider {
    override val supportedProfile: OperationalProfile = OperationalProfile.BASIC
    override val isHardwareBacked: Boolean = false
    override val providerName: String = "SoftwareKeyProvider (JCA)"

    private val keyPairs = ConcurrentHashMap<String, KeyPair>()
    private val secretKeys = ConcurrentHashMap<String, SecretKey>()

    override fun generateKeyPair(alias: String): Boolean {
        return try {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(256)
            val kp = kpg.generateKeyPair()
            keyPairs[alias] = kp

            val kg = KeyGenerator.getInstance("AES")
            kg.init(256)
            secretKeys[alias] = kg.generateKey()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun sign(alias: String, data: ByteArray): ByteArray {
        val kp = keyPairs[alias] ?: run {
            generateKeyPair(alias)
            keyPairs[alias]!!
        }
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(kp.private)
        sig.update(data)
        return sig.sign()
    }

    override fun verify(alias: String, data: ByteArray, signature: ByteArray): Boolean {
        val kp = keyPairs[alias] ?: return false
        return try {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(kp.public)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }

    override fun encrypt(alias: String, plainText: ByteArray): ByteArray {
        val key = secretKeys[alias] ?: run {
            generateKeyPair(alias)
            secretKeys[alias]!!
        }
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(plainText)
    }

    override fun decrypt(alias: String, cipherText: ByteArray): ByteArray {
        val key = secretKeys[alias] ?: return plainTextFallback(cipherText)
        return try {
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, key)
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            plainTextFallback(cipherText)
        }
    }

    private fun plainTextFallback(bytes: ByteArray): ByteArray = bytes
}
