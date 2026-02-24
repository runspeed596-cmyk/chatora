package com.chatora.app.data.repositories

import android.content.Context
import com.chatora.app.data.local.UserDao
import com.chatora.app.data.remote.*
import com.chatora.app.domain.repositories.AuthRepository
import com.chatora.app.data.local.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun loginWithDevice(): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(getDeviceId()))
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> {
        return try {
            val response = apiService.googleLogin(GoogleLoginRequest(idToken = idToken, deviceId = getDeviceId()))
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.emailLogin(EmailLoginRequest(email = email, password = password, deviceId = getDeviceId()))
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerWithEmail(email: String, password: String, username: String): Result<AuthResponse> {
        return try {
            val response = apiService.emailRegister(EmailRegisterRequest(email = email, password = password, username = username, deviceId = getDeviceId()))
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyEmail(email: String, code: String): Result<AuthResponse> {
        return try {
            val response = apiService.verifyEmail(VerifyEmailRequest(email = email, code = code))
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resendCode(email: String): Result<Map<String, String>> {
        return try {
            val response = apiService.resendCode(EmailResendRequest(email = email))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    val errorMsg = body?.error?.message ?: body?.message ?: "Unknown Error"
                    val errorCode = body?.error?.code
                    Result.failure(ApiException(errorCode, errorMsg))
                }
            } else {
                val errorResult = NetworkUtils.parseErrorResponse(response)
                Result.failure(ApiException(errorResult.code, errorResult.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAccessToken(): Flow<String?> = flow {
        emit(tokenManager.getToken())
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        userDao.clearAll()
    }

    private fun getDeviceId(): String {
        // Mock device ID for now, in real app use Secure.ANDROID_ID or similar
        return "device_123456"
    }

    private fun handleResponse(response: retrofit2.Response<ApiResponse<AuthResponse>>): Result<AuthResponse> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                tokenManager.saveToken(body.data.accessToken)
                tokenManager.saveRefreshToken(body.data.refreshToken)
                
                GlobalScope.launch {
                    try {
                        userDao.insertUser(
                            com.chatora.app.data.local.UserEntity(
                                id = "me",
                                username = body.data.username,
                                email = "user@minichat.com",
                                karma = 0,
                                diamonds = 0,
                                countryCode = "US",
                                countryNameRes = com.chatora.app.R.string.country_us,
                                countryFlag = "🇺🇸",
                                isPremium = false
                            )
                        )
                    } catch (e: Exception) {
                        // Ignore local save errors
                    }
                }
                
                Result.success(body.data)
            } else {
                val errorMsg = body?.error?.message ?: body?.message ?: "Unknown Error"
                val errorCode = body?.error?.code
                Result.failure(ApiException(errorCode, errorMsg))
            }
        } else {
            val errorResult = NetworkUtils.parseErrorResponse(response)
            Result.failure(ApiException(errorResult.code, errorResult.message))
        }
    }
}
