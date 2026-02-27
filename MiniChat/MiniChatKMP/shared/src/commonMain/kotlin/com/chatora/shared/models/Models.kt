package com.chatora.shared.models

import kotlinx.serialization.Serializable

// ─── API Response Wrapper ─────────────────────────────────────────────────────

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

// ─── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val deviceId: String,
    val username: String? = null,
    val password: String? = null,
    val countryCode: String? = null,
    val languageCode: String = "en",
    val fcmToken: String? = null
)

@Serializable
data class EmailLoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

@Serializable
data class EmailRegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val deviceId: String,
    val countryCode: String? = null,
    val languageCode: String = "en"
)

@Serializable
data class GoogleLoginRequest(
    val idToken: String,
    val deviceId: String,
    val countryCode: String? = null,
    val languageCode: String = "en"
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val tempUsername: Boolean,
    val emailVerified: Boolean = true,
    val verificationCode: String? = null
)

// ─── User ─────────────────────────────────────────────────────────────────────

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val email: String? = null,
    val isPremium: Boolean = false,
    val gender: String = "UNSPECIFIED",
    val countryCode: String? = null,
    val karma: Int = 50
)

// ─── Chat ─────────────────────────────────────────────────────────────────────

@Serializable
data class ChatMessage(
    val sender: String,
    val message: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val timestamp: Long = 0L
)

@Serializable
data class ChatMessageRequest(
    val message: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)

// ─── Match ────────────────────────────────────────────────────────────────────

@Serializable
data class MatchEvent(
    val type: String = "MATCH_FOUND",
    val matchId: String = "",
    val partnerId: String = "",
    val partnerUsername: String = "",
    val initiator: Boolean = false,
    val partnerIp: String = "",
    val partnerCountryCode: String = ""
)

@Serializable
data class JoinQueueRequest(
    val userId: String,
    val username: String,
    val myCountry: String,
    val targetCountry: String,
    val targetGender: String,
    val lang: String,
    val sessionId: String,
    val isPremium: Boolean,
    val gender: String,
    val karma: Int,
    val ipAddress: String
)

// ─── Subscription ─────────────────────────────────────────────────────────────

@Serializable
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val durationMonths: Int,
    val price: Double,
    val currency: String = "USD",
    val features: List<String> = emptyList()
)
