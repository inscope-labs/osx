package com.inscopelabs.osx.di

import android.content.Context
import com.inscopelabs.osx.data.crypto.AndroidKeyStoreProvider
import com.inscopelabs.osx.data.crypto.CryptoProvider
import com.inscopelabs.osx.data.crypto.SoftwareKeyProvider
import com.inscopelabs.osx.data.crypto.TeeKeyProvider
import com.inscopelabs.osx.domain.key.ConsoleKeyManager
import com.inscopelabs.osx.domain.key.KeyHierarchy
import com.inscopelabs.osx.domain.profile.OperationalProfile

class CryptoModule(private val context: Context) {
    val keyHierarchy: KeyHierarchy by lazy { KeyHierarchy() }
    val keyManager: ConsoleKeyManager by lazy { ConsoleKeyManager(keyHierarchy) }

    val softwareCryptoProvider: SoftwareKeyProvider by lazy { SoftwareKeyProvider() }
    val androidKeyStoreProvider: AndroidKeyStoreProvider by lazy { AndroidKeyStoreProvider() }
    val teeKeyProvider: TeeKeyProvider by lazy { TeeKeyProvider(context) }

    fun getCryptoProviderForProfile(profile: OperationalProfile): CryptoProvider {
        return when (profile) {
            OperationalProfile.BASIC -> softwareCryptoProvider
            OperationalProfile.STANDARD -> androidKeyStoreProvider
            OperationalProfile.HIGH -> teeKeyProvider
        }
    }
}
