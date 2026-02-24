package com.chatora.app.data.remote

import android.util.Log
import com.chatora.app.BuildConfig
import com.chatora.app.data.local.TokenManager
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that attaches the JWT access token to requests
 * and automatically refreshes it when a 401 response is received.
 *
 * Refresh flow:
 * 1. Original request returns 401
 * 2. Send refresh token to /auth/refresh
 * 3. If successful, save new tokens and retry original request
 * 4. If refresh fails, clear tokens (forcing re-login)
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for auth endpoints (login, register, refresh)
        if (originalRequest.url.encodedPath.startsWith("/auth/")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenManager.getToken()
        val request = originalRequest.newBuilder().apply {
            if (!token.isNullOrEmpty()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(request)

        // If we get 401/403, try to refresh the token
        if (response.code == 401 || response.code == 403) {
            val refreshToken = tokenManager.getRefreshToken()
            if (!refreshToken.isNullOrEmpty()) {
                response.close()
                return attemptTokenRefresh(chain, originalRequest, refreshToken)
            }
        }

        return response
    }

    private fun attemptTokenRefresh(
        chain: Interceptor.Chain,
        originalRequest: Request,
        refreshToken: String
    ): Response {
        try {
            Log.d(TAG, "Attempting token refresh...")

            val refreshBody = Gson().toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())

            val refreshRequest = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "auth/refresh")
                .post(refreshBody)
                .build()

            val refreshResponse = chain.proceed(refreshRequest)

            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body?.string()
                refreshResponse.close()

                if (body != null) {
                    val parsed = Gson().fromJson(body, RefreshApiResponse::class.java)
                    if (parsed.success && parsed.data != null) {
                        // Save new tokens
                        tokenManager.saveToken(parsed.data.accessToken)
                        tokenManager.saveRefreshToken(parsed.data.refreshToken)
                        Log.d(TAG, "Token refreshed successfully")

                        // Retry original request with new token
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer ${parsed.data.accessToken}")
                            .build()
                        return chain.proceed(newRequest)
                    }
                }
            } else {
                refreshResponse.close()
            }

            // Refresh failed — clear tokens (user must re-login)
            Log.w(TAG, "Token refresh failed, clearing tokens")
            tokenManager.clearToken()

        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            tokenManager.clearToken()
        }

        // Fallback: retry original request (will fail with 401, triggering login screen)
        val fallbackRequest = originalRequest.newBuilder().build()
        return chain.proceed(fallbackRequest)
    }

    /** Internal DTO for parsing refresh response */
    private data class RefreshApiResponse(
        val success: Boolean,
        val data: RefreshData?,
        val message: String?
    )

    private data class RefreshData(
        val accessToken: String,
        val refreshToken: String
    )
}
