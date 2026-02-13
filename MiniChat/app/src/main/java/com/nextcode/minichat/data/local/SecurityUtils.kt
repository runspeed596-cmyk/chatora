package com.nextcode.minichat.data.local

import android.content.Context
import android.os.Build
import android.os.Debug
import java.io.File

/**
 * Security utilities for runtime environment checks.
 * Detects rooted devices, emulators, and debugger attachment.
 */
object SecurityUtils {

    /**
     * Checks if the device appears to be rooted.
     * Returns true if any common root indicators are found.
     */
    fun isRooted(): Boolean {
        return checkRootBinaries() || checkSuExists() || checkDangerousProps()
    }

    /**
     * Checks if the app is running on an emulator.
     * Returns true if emulator indicators are detected.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.BOARD == "QC_Reference_Phone"
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * Checks if a debugger is currently attached.
     */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Performs a comprehensive security check.
     * Returns a SecurityStatus with all check results.
     */
    fun performSecurityCheck(): SecurityStatus {
        return SecurityStatus(
            isRooted = isRooted(),
            isEmulator = isEmulator(),
            isDebuggerAttached = isDebuggerAttached()
        )
    }

    // ─── Private Helpers ───

    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/app/SuperSU.apk",
            "/system/app/SuperSU",
            "/system/app/Superuser",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99telecominfra"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkSuExists(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readText().trim()
            process.destroy()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun checkDangerousProps(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.debuggable"))
            val result = process.inputStream.bufferedReader().readText().trim()
            process.destroy()
            result == "1"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Data class containing results of all security checks.
     */
    data class SecurityStatus(
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggerAttached: Boolean
    ) {
        /** True if any security concern was detected */
        val hasSecurityConcern: Boolean
            get() = isRooted || isDebuggerAttached
    }
}
