package com.chatora.app.data.remote

import com.chatora.app.BuildConfig

/**
 * API connection constants.
 * URLs are provided via BuildConfig fields defined in build.gradle.kts.
 * Debug builds use HTTP to local dev server.
 * Release builds use HTTPS to production server.
 */
object ApiConstants {
    val BASE_URL: String get() = BuildConfig.API_BASE_URL
    val WS_URL: String get() = BuildConfig.WS_URL
}
