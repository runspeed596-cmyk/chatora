package com.iliyadev.minichat.api.dtos

import com.iliyadev.minichat.domain.entities.PaymentStatus
import java.time.LocalDateTime
import java.util.UUID

data class AdminStatsDto(
    val totalUsers: Long,
    val dailyActiveUsers: Long,
    val dailyVisits: Long,
    val dailyIncome: Double,
    val totalRevenue: Double,
    val monthlyRevenue: Double,
    val successfulPayments: Long,
    val failedPayments: Long
)

data class AdminUserDto(
    val id: String,
    val username: String,
    val email: String?,
    val role: String,
    val status: String, // ACTIVE, BLOCKED
    val subscriptionType: String,
    val isPremium: Boolean,
    val premiumUntil: LocalDateTime?,
    val registrationDate: LocalDateTime,
    val lastLogin: LocalDateTime,
    val ipAddress: String?
)

data class AdminUserListResponse(
    val users: List<AdminUserDto>,
    val total: Long,
    val page: Int,
    val totalPages: Int
)

data class CreateUserRequest(
    val username: String,
    val email: String?,
    val password: String,
    val role: String = "USER",
    val gender: String = "UNSPECIFIED"
)

data class AdminTransactionDto(
    val id: String,
    val userId: String,
    val username: String,
    val amount: Double,
    val currency: String,
    val status: PaymentStatus,
    val date: LocalDateTime,
    val paymentMethod: String,
    val orderId: String
)

data class RevenueDataDto(
    val date: String,
    val amount: Double
)

data class CountryDataDto(
    val country: String,
    val code: String,
    val count: Long
)

/**
 * Ephemeral active match data for admin monitoring.
 * NOT persisted to database — lives only in server memory while match is active.
 */
data class ActiveMatchDto(
    val matchId: String,
    val user1: String,
    val user2: String,
    val startedAt: Long,
    val lastMessage: String? = null
)

