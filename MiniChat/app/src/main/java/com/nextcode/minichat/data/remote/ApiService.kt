package com.nextcode.minichat.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.MultipartBody
import retrofit2.http.Header
import retrofit2.http.Path


// Auth DTOs

// Auth DTOs
data class LoginRequest(val deviceId: String)

data class GoogleLoginRequest(
    val idToken: String,
    val deviceId: String,
    val countryCode: String = "US",
    val languageCode: String = "en"
)

data class EmailLoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

data class EmailRegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val deviceId: String,
    val countryCode: String = "US",
    val languageCode: String = "en"
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class EmailResendRequest(
    val email: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val tempUsername: Boolean,
    val emailVerified: Boolean = true
)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/email/login")
    suspend fun emailLogin(@Body request: EmailLoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/email/register")
    suspend fun emailRegister(@Body request: EmailRegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/email/resend-code")
    suspend fun resendCode(@Body request: EmailResendRequest): Response<ApiResponse<String>>

    @GET("users/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(
        @Part file: okhttp3.MultipartBody.Part,
        @Header("X-Match-Id") matchId: String?
    ): Response<Map<String, String>>

    @POST("api/files/cleanup/{matchId}")
    suspend fun cleanupMatchFiles(@Path("matchId") matchId: String): Response<Map<String, String>>

    @POST("users/gender")
    suspend fun updateGender(@Body request: Map<String, String>): Response<ApiResponse<String>>
}
