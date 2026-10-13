package com.inscopelabs.osx.data.local

import android.content.Context
import android.content.SharedPreferences
import com.inscopelabs.osx.domain.profile.OperationalProfile

/**
 * Secure persistent preference store for console operator state.
 */
class SecureStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("osx_console_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_OPERATOR_ID = "operator_id"
        private const val KEY_PROFILE = "active_profile"
        private const val KEY_CONTROLLER_ENDPOINT = "controller_endpoint"
        private const val KEY_AUTO_VERIFY = "auto_verify_envelopes"
    }

    var operatorId: String
        get() = prefs.getString(KEY_OPERATOR_ID, "OP-ALPHA-701") ?: "OP-ALPHA-701"
        set(value) = prefs.edit().putString(KEY_OPERATOR_ID, value).apply()

    var activeProfile: OperationalProfile
        get() {
            val name = prefs.getString(KEY_PROFILE, OperationalProfile.STANDARD.name)
            return try {
                OperationalProfile.valueOf(name ?: OperationalProfile.STANDARD.name)
            } catch (e: Exception) {
                OperationalProfile.STANDARD
            }
        }
        set(value) = prefs.edit().putString(KEY_PROFILE, value.name).apply()

    var controllerEndpoint: String
        get() = prefs.getString(KEY_CONTROLLER_ENDPOINT, "wss://ctrl.osx10.inscope.network:9443/v1") ?: "wss://ctrl.osx10.inscope.network:9443/v1"
        set(value) = prefs.edit().putString(KEY_CONTROLLER_ENDPOINT, value).apply()

    var autoVerifyEnvelopes: Boolean
        get() = prefs.getBoolean(KEY_AUTO_VERIFY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_VERIFY, value).apply()
}
