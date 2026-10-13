package com.inscopelabs.osx.data.crypto

import com.inscopelabs.osx.domain.profile.OperationalProfile

interface CryptoProvider {
    val supportedProfile: OperationalProfile
    val isHardwareBacked: Boolean
    val providerName: String

    fun generateKeyPair(alias: String): Boolean
    fun sign(alias: String, data: ByteArray): ByteArray
    fun verify(alias: String, data: ByteArray, signature: ByteArray): Boolean
    fun encrypt(alias: String, plainText: ByteArray): ByteArray
    fun decrypt(alias: String, cipherText: ByteArray): ByteArray
}
