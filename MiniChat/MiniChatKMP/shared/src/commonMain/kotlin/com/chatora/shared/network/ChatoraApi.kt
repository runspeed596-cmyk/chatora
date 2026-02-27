package com.chatora.shared.network

import com.chatora.shared.models.ApiResponse
import com.chatora.shared.models.AuthResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Centralized API client for all Chatora backend communication.
 * Platform-specific HTTP engines are configured via Koin DI.
 */
class ChatoraApi(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {

    // ─── Auth Endpoints ──────────────────────────────────────────────

    suspend fun login(deviceId: String, username: String?, password: String?): ApiResponse<AuthResponse> {
        return httpClient.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "deviceId" to deviceId,
                "username" to username,
                "password" to password
            ))
        }.body()
    }

    suspend fun emailLogin(email: String, password: String, deviceId: String): ApiResponse<AuthResponse> {
        return httpClient.post("$baseUrl/auth/email/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "email" to email,
                "password" to password,
                "deviceId" to deviceId
            ))
        }.body()
    }

    suspend fun emailRegister(
        email: String,
        password: String,
        username: String,
        deviceId: String,
        countryCode: String? = null,
        languageCode: String = "en"
    ): ApiResponse<AuthResponse> {
        return httpClient.post("$baseUrl/auth/email/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "email" to email,
                "password" to password,
                "username" to username,
                "deviceId" to deviceId,
                "countryCode" to countryCode,
                "languageCode" to languageCode
            ))
        }.body()
    }

    suspend fun verifyEmail(email: String, code: String): ApiResponse<AuthResponse> {
        return httpClient.post("$baseUrl/auth/verify-email") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "email" to email,
                "code" to code
            ))
        }.body()
    }

    suspend fun googleLogin(idToken: String, deviceId: String): ApiResponse<AuthResponse> {
        return httpClient.post("$baseUrl/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "idToken" to idToken,
                "deviceId" to deviceId
            ))
        }.body()
    }

    suspend fun refreshToken(refreshToken: String): ApiResponse<AuthResponse> {
        return httpClient.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }.body()
    }

    // ─── User Endpoints ──────────────────────────────────────────────

    suspend fun getUserProfile(token: String): ApiResponse<Map<String, String>> {
        return httpClient.get("$baseUrl/user/me") {
            bearerAuth(token)
        }.body()
    }

    suspend fun updateProfile(token: String, updates: Map<String, String>): ApiResponse<Map<String, String>> {
        return httpClient.put("$baseUrl/user/me") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(updates)
        }.body()
    }

    // ─── Health Check ────────────────────────────────────────────────

    suspend fun healthCheck(): ApiResponse<Map<String, String>> {
        return httpClient.get("$baseUrl/auth/health").body()
    }
}
