package com.chatora.shared.repository

import com.chatora.shared.models.*
import com.chatora.shared.network.ChatoraApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication repository — handles login, registration, token management.
 * Tokens are stored in-memory; platform-specific secure storage can be added later.
 */
class AuthRepository(
    private val api: ChatoraApi
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState

    private var accessToken: String? = null
    private var refreshTokenValue: String? = null

    val isLoggedIn: Boolean get() = accessToken != null

    suspend fun loginWithDevice(deviceId: String): Result<AuthResponse> {
        return try {
            val response = api.login(deviceId, null, null)
            if (response.success && response.data != null) {
                saveTokens(response.data)
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(email: String, password: String, deviceId: String): Result<AuthResponse> {
        return try {
            val response = api.emailLogin(email, password, deviceId)
            if (response.success && response.data != null) {
                saveTokens(response.data)
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Email login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String,
        deviceId: String
    ): Result<AuthResponse> {
        return try {
            val response = api.emailRegister(email, password, username, deviceId)
            if (response.success && response.data != null) {
                saveTokens(response.data)
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEmail(email: String, code: String): Result<AuthResponse> {
        return try {
            val response = api.verifyEmail(email, code)
            if (response.success && response.data != null) {
                saveTokens(response.data)
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<AuthResponse> {
        val token = refreshTokenValue ?: return Result.failure(Exception("No refresh token"))
        return try {
            val response = api.refreshToken(token)
            if (response.success && response.data != null) {
                saveTokens(response.data)
                Result.success(response.data)
            } else {
                _authState.value = AuthState.LoggedOut
                Result.failure(Exception(response.error ?: "Token refresh failed"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.LoggedOut
            Result.failure(e)
        }
    }

    fun logout() {
        accessToken = null
        refreshTokenValue = null
        _authState.value = AuthState.LoggedOut
    }

    fun getAccessToken(): String? = accessToken

    private fun saveTokens(auth: AuthResponse) {
        accessToken = auth.accessToken
        refreshTokenValue = auth.refreshToken
        _authState.value = AuthState.LoggedIn(
            userId = auth.userId,
            username = auth.username,
            emailVerified = auth.emailVerified
        )
    }
}

sealed class AuthState {
    data object LoggedOut : AuthState()
    data object Loading : AuthState()
    data class LoggedIn(
        val userId: String,
        val username: String,
        val emailVerified: Boolean
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}
