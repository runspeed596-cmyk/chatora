package com.nextcode.minichat.domain.repositories

import com.nextcode.minichat.data.remote.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun loginWithDevice(): Result<AuthResponse>
    suspend fun loginWithGoogle(idToken: String): Result<AuthResponse>
    suspend fun loginWithEmail(email: String, password: String): Result<AuthResponse>
    suspend fun registerWithEmail(email: String, password: String, username: String): Result<AuthResponse>
    suspend fun verifyEmail(email: String, code: String): Result<AuthResponse>
    suspend fun resendCode(email: String): Result<String>
    fun getAccessToken(): Flow<String?>
    suspend fun logout()
}
