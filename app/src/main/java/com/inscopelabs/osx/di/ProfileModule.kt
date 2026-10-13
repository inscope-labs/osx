package com.inscopelabs.osx.di

import com.inscopelabs.osx.data.local.SecureStorage
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.profile.ProfileSelector

class ProfileModule(private val secureStorage: SecureStorage) {
    val profileSelector: ProfileSelector by lazy {
        ProfileSelector(secureStorage.activeProfile)
    }

    fun syncSelectedProfile(profile: OperationalProfile) {
        secureStorage.activeProfile = profile
    }
}
