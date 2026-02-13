package com.nextcode.minichat.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nextcode.minichat.data.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication tokens and app preferences.
 * Sensitive data (tokens) stored in EncryptedSharedPreferences.
 * Non-sensitive preferences (theme, onboarding) stored in regular SharedPreferences.
 */
@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {

    /** Encrypted storage for security-critical data (tokens, device ID) */
    private val securePrefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback: if EncryptedSharedPreferences fails (rare edge case on some devices),
        // use regular prefs to avoid crash. LOG this for investigation.
        android.util.Log.e("TokenManager", "EncryptedSharedPreferences init failed, using fallback", e)
        context.getSharedPreferences("auth_prefs_fallback", Context.MODE_PRIVATE)
    }

    /** Regular storage for non-sensitive preferences */
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // ─── Token Management (Encrypted) ───

    fun saveToken(token: String?) {
        securePrefs.edit().putString("access_token", token).apply()
    }

    fun getToken(): String? {
        return securePrefs.getString("access_token", null)
    }

    fun clearToken() {
        securePrefs.edit().remove("access_token").apply()
    }

    // ─── Device ID (Encrypted) ───

    fun saveDeviceId(deviceId: String) {
        securePrefs.edit().putString("device_id", deviceId).apply()
    }

    fun getDeviceId(): String? {
        return securePrefs.getString("device_id", null)
    }

    // ─── Non-Sensitive Preferences ───

    fun saveOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean("onboarding_seen", seen).apply()
    }

    fun isOnboardingSeen(): Boolean {
        return prefs.getBoolean("onboarding_seen", false)
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun getThemeMode(): ThemeMode {
        val modeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeStr ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    /** Clear all secure data (for logout) */
    fun clearAll() {
        securePrefs.edit().clear().apply()
    }
}
