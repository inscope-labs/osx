package com.inscopelabs.osx.data.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.inscopelabs.osx.domain.profile.OperationalProfile
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Hardware TEE / StrongBox-backed cryptographic provider for HIGH Operational Profile.
 */
class TeeKeyProvider(
    private val context: Context? = null
) : CryptoProvider {
    override val supportedProfile: OperationalProfile = OperationalProfile.HIGH
    override val isHardwareBacked: Boolean = true
    override val providerName: String = "TeeKeyProvider (StrongBox/Hardware TEE)"

    private val fallbackKeyStoreProvider = AndroidKeyStoreProvider()

    fun isStrongBoxSupported(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && context != null) {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun generateKeyPair(alias: String): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(alias)) return true

            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isStrongBoxSupported()) {
                builder.setIsStrongBoxBacked(true)
            }

            kpg.initialize(builder.build())
            kpg.generateKeyPair()
            true
        } catch (e: Exception) {
            // Fallback to standard keystore
            fallbackKeyStoreProvider.generateKeyPair(alias)
        }
    }

    override fun sign(alias: String, data: ByteArray): ByteArray {
        return fallbackKeyStoreProvider.sign(alias, data)
    }

    override fun verify(alias: String, data: ByteArray, signature: ByteArray): Boolean {
        return fallbackKeyStoreProvider.verify(alias, data, signature)
    }

    override fun encrypt(alias: String, plainText: ByteArray): ByteArray {
        return fallbackKeyStoreProvider.encrypt(alias, plainText)
    }

    override fun decrypt(alias: String, cipherText: ByteArray): ByteArray {
        return fallbackKeyStoreProvider.decrypt(alias, cipherText)
    }
}
