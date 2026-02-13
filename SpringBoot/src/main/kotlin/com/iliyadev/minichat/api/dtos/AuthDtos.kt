package com.iliyadev.minichat.api.dtos

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    val deviceId: String? = null,
    val username: String? = null,
    val password: String? = null,
    val countryCode: String = "US",
    val languageCode: String = "en",
    val fcmToken: String? = null
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val tempUsername: Boolean,
    val emailVerified: Boolean = true,
    val verificationCode: String? = null
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String
)

data class GoogleLoginRequest(
    @field:NotBlank
    val idToken: String,
    @field:NotBlank
    val deviceId: String,
    val countryCode: String = "US",
    val languageCode: String = "en"
)

data class EmailLoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String,
    @field:NotBlank
    val deviceId: String
)

data class EmailRegisterRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 6)
    val password: String,
    @field:NotBlank
    @field:Size(min = 3, max = 20)
    val username: String,
    @field:NotBlank
    val deviceId: String,
    val countryCode: String = "US",
    val languageCode: String = "en"
)

data class VerifyEmailRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 6, max = 6)
    val code: String
)

data class EmailResendRequest(
    @field:Email
    @field:NotBlank
    val email: String
)
