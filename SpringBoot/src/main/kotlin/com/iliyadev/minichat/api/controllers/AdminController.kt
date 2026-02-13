package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.api.dtos.*
import com.iliyadev.minichat.core.response.ApiResponse
import com.iliyadev.minichat.domain.entities.PaymentStatus
import com.iliyadev.minichat.domain.entities.Role
import com.iliyadev.minichat.domain.entities.SubscriptionPlan
import com.iliyadev.minichat.domain.repositories.PaymentTransactionRepository
import com.iliyadev.minichat.domain.repositories.SubscriptionPlanRepository
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/admin")
class AdminController(
    private val userRepository: UserRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val paymentTransactionRepository: PaymentTransactionRepository
) {

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/stats")
    fun getStats(): ApiResponse<AdminStatsDto> {
        val totalUsers = userRepository.count()
        val totalRevenue = paymentTransactionRepository.sumAmountByStatus(PaymentStatus.SUCCESS) ?: 0.0
        
        val oneDayAgo = LocalDateTime.now().minusDays(1)
        val dailyActive = userRepository.countByUpdatedAtAfter(oneDayAgo)
        
        val todayMidnight = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
        val dailyIncome = paymentTransactionRepository.sumAmountByStatusAndUpdatedAtAfter(PaymentStatus.SUCCESS, todayMidnight) ?: 0.0

        val oneMonthAgo = LocalDateTime.now().minusMonths(1)
        val monthlyRevenue = paymentTransactionRepository.sumAmountByStatusAndUpdatedAtAfter(PaymentStatus.SUCCESS, oneMonthAgo) ?: 0.0



        return ApiResponse.success(
            AdminStatsDto(
                totalUsers = totalUsers,
                dailyActiveUsers = dailyActive,
                dailyVisits = dailyActive * 3, // Approximation logic
                dailyIncome = dailyIncome,
                totalRevenue = totalRevenue,
                monthlyRevenue = monthlyRevenue,
                successfulPayments = paymentTransactionRepository.countByStatus(PaymentStatus.SUCCESS),
                failedPayments = paymentTransactionRepository.countByStatus(PaymentStatus.FAILED)
            )
        )
    }

    @GetMapping("/users")
    fun getUsers(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?
    ): ApiResponse<AdminUserListResponse> {
        val pageNumber = if (page > 0) page - 1 else 0
        val pageable = PageRequest.of(pageNumber, limit, Sort.by("createdAt").descending())
        val userPage = userRepository.findAll(pageable)
        
        val dtos = userPage.content.map { user ->
            AdminUserDto(
                id = user.id.toString(),
                username = user.username,
                email = user.email,
                role = user.role?.name ?: "USER",
                status = if (user.isBanned) "BLOCKED" else "ACTIVE",
                subscriptionType = if (user.isPremium) "PREMIUM" else "FREE",
                registrationDate = user.createdAt,
                lastLogin = user.updatedAt,
                ipAddress = "127.0.0.1"
            )
        }

        return ApiResponse.success(
            AdminUserListResponse(
                users = dtos,
                total = userPage.totalElements,
                page = page,
                totalPages = userPage.totalPages
            )
        )
    }

    @PostMapping("/users/{id}/block")
    fun blockUser(@PathVariable id: UUID): ApiResponse<String> {
        val user = userRepository.findById(id).orElseThrow { RuntimeException("User not found") }
        user.isBanned = true
        userRepository.save(user)
        return ApiResponse.success("User blocked")
    }

    @PostMapping("/users/{id}/unblock")
    fun unblockUser(@PathVariable id: UUID): ApiResponse<String> {
        val user = userRepository.findById(id).orElseThrow { RuntimeException("User not found") }
        user.isBanned = false
        userRepository.save(user)
        return ApiResponse.success("User unblocked")
    }
    
    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: UUID): ApiResponse<String> {
        userRepository.deleteById(id)
        return ApiResponse.success("User deleted")
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/transactions")
    fun getTransactions(): ApiResponse<List<AdminTransactionDto>> {
        val txns = paymentTransactionRepository.findAll(Sort.by("createdAt").descending())
        val dtos = txns.take(50).map { txn ->
            AdminTransactionDto(
                id = txn.id.toString(),
                userId = txn.user.id.toString(),
                username = txn.user.username,
                amount = txn.amount,
                currency = txn.currency,
                status = txn.status,
                date = txn.createdAt,
                paymentMethod = "1xGate",
                orderId = txn.orderId
            )
        }
        return ApiResponse.success(dtos)
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/stats/revenue-history")
    fun getRevenueHistory(): ApiResponse<List<RevenueDataDto>> {
        val oneWeekAgo = LocalDateTime.now().minusDays(7)
        // Optimization: Use findAll with date filter if possible, but for 7 days simple is fine
        val transactions = paymentTransactionRepository.findAll()
            .filter { it.status == PaymentStatus.SUCCESS && it.updatedAt.isAfter(oneWeekAgo) }
        
        val map = transactions.groupBy { it.createdAt.toLocalDate().toString() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            
        val result = mutableListOf<RevenueDataDto>()
        for (i in 6 downTo 0) {
            val date = LocalDateTime.now().minusDays(i.toLong()).toLocalDate().toString()
            val amount = map[date] ?: 0.0
            result.add(RevenueDataDto(date, amount))
        }

        return ApiResponse.success(result)
    }

    @GetMapping("/stats/countries")
    fun getTopCountries(): ApiResponse<List<CountryDataDto>> {
        val stats = userRepository.countByCountryCode()
        
        val dtos = stats.map { 
             val code = it[0] as String
             val count = it[1] as Long
             CountryDataDto(
                 country = Locale("", code).getDisplayCountry(Locale.ENGLISH) ?: code,
                 code = code,
                 count = count
             )
        }.sortedByDescending { it.count }.take(10)
        
        return ApiResponse.success(dtos)
    }

    @GetMapping("/subscriptions")
    fun getSubscriptions(): ApiResponse<List<SubscriptionPlan>> {
        return ApiResponse.success(subscriptionPlanRepository.findAll())
    }

    @PutMapping("/plans/{id}")
    fun updatePlan(@PathVariable id: UUID, @RequestBody request: Map<String, Any>): ApiResponse<String> {
        val plan = subscriptionPlanRepository.findById(id).orElseThrow { RuntimeException("Plan not found") }
        val newPrice = request["price"]?.toString()?.toDoubleOrNull() ?: throw RuntimeException("Invalid price")
        plan.priceUsd = newPrice
        subscriptionPlanRepository.save(plan)
        return ApiResponse.success("Plan updated successfully")
    }
}
