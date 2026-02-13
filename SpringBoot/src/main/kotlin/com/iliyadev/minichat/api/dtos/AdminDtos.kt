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
