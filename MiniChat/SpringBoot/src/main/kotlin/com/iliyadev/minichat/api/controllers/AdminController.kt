package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.api.dtos.*
import com.iliyadev.minichat.core.response.ApiResponse
import com.iliyadev.minichat.domain.entities.Gender
import com.iliyadev.minichat.domain.entities.PaymentStatus
import com.iliyadev.minichat.domain.entities.Role
import com.iliyadev.minichat.domain.entities.SubscriptionPlan
import com.iliyadev.minichat.domain.entities.User
import com.iliyadev.minichat.domain.repositories.PaymentTransactionRepository
import com.iliyadev.minichat.domain.repositories.SubscriptionPlanRepository
import com.iliyadev.minichat.domain.repositories.UserRepository
import com.iliyadev.minichat.domain.services.MatchService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = ["*"])
class AdminController(
    private val userRepository: UserRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val matchService: MatchService
) {

    // ─── Stats ───────────────────────────────────────────────────────────────────

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
                failedPayments = paymentTransactionRepository.count() - paymentTransactionRepository.countByStatus(PaymentStatus.SUCCESS)
            )
        )
    }

    // ─── Users ───────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    fun getUsers(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?
    ): ApiResponse<AdminUserListResponse> {
        val pageNumber = if (page > 0) page - 1 else 0
        val pageable = PageRequest.of(pageNumber, limit, Sort.by("createdAt").descending())

        val hasSearch = !search.isNullOrBlank()
        val isBanned = status == "BLOCKED"
        val hasStatus = status != null && status != "ALL"

        val userPage = when {
            hasSearch && hasStatus -> userRepository.searchByUsernameOrEmailAndIsBanned(search!!, isBanned, pageable)
            hasSearch -> userRepository.searchByUsernameOrEmail(search!!, pageable)
            hasStatus -> userRepository.findByIsBanned(isBanned, pageable)
            else -> userRepository.findAll(pageable)
        }
        
        val dtos = userPage.content.map { user -> user.toAdminDto() }

        return ApiResponse.success(
            AdminUserListResponse(
                users = dtos,
                total = userPage.totalElements,
                page = page,
                totalPages = userPage.totalPages
            )
        )
    }

    @PostMapping("/users")
    fun createUser(@RequestBody request: CreateUserRequest): ApiResponse<AdminUserDto> {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.username)) {
            throw RuntimeException("Username '${request.username}' already exists")
        }
        if (!request.email.isNullOrBlank() && userRepository.existsByEmail(request.email)) {
            throw RuntimeException("Email '${request.email}' already exists")
        }

        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            deviceId = "admin-created-${UUID.randomUUID()}",
            role = if (request.role == "ADMIN") Role.ADMIN else Role.USER,
            gender = try { Gender.valueOf(request.gender.uppercase()) } catch (_: Exception) { Gender.UNSPECIFIED }
        )

        val savedUser = userRepository.save(user)
        return ApiResponse.success(savedUser.toAdminDto())
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

    @PostMapping("/users/{id}/upgrade")
    fun upgradeUser(@PathVariable id: UUID): ApiResponse<AdminUserDto> {
        val user = userRepository.findById(id).orElseThrow { RuntimeException("User not found") }
        user.isPremium = true
        user.premiumUntil = LocalDateTime.now().plusDays(30)
        val saved = userRepository.save(user)
        return ApiResponse.success(saved.toAdminDto())
    }

    @PostMapping("/users/{id}/downgrade")
    fun downgradeUser(@PathVariable id: UUID): ApiResponse<AdminUserDto> {
        val user = userRepository.findById(id).orElseThrow { RuntimeException("User not found") }
        user.isPremium = false
        user.premiumUntil = null
        val saved = userRepository.save(user)
        return ApiResponse.success(saved.toAdminDto())
    }

    // ─── Transactions ────────────────────────────────────────────────────────────

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
        
        val dtos = stats
            .filter { (it[0] as? String)?.isNotBlank() == true }
            .map { 
                val code = it[0] as String
                val count = it[1] as Long
                CountryDataDto(
                    country = Locale("", code).getDisplayCountry(Locale.ENGLISH).ifBlank { code },
                    code = code,
                    count = count
                )
            }
            .sortedByDescending { it.count }
            .take(10)
        
        return ApiResponse.success(dtos)
    }

    // ─── Active Chats (Ephemeral — In-Memory Only) ────────────────────────────────

    /**
     * Returns currently active video chat matches.
     * Data is ephemeral — lives only in server memory while matches are active.
     * No database writes. Matches disappear instantly when users disconnect or press findNext.
     */
    @GetMapping("/active-chats")
    fun getActiveChats(): ApiResponse<List<ActiveMatchDto>> {
        val matches = matchService.getActiveMatches().map { info ->
            ActiveMatchDto(
                matchId = info.matchId,
                user1 = info.user1,
                user2 = info.user2,
                startedAt = info.startedAt,
                lastMessage = info.lastMessage
            )
        }
        return ApiResponse.success(matches)
    }

    /**
     * Returns history of messages for an active match.
     */
    @GetMapping("/active-chats/{matchId}/messages")
    fun getMatchMessages(@PathVariable matchId: String): ApiResponse<List<ChatMessageResponse>> {
        val messages = matchService.getMatchMessages(matchId)
        return ApiResponse.success(messages)
    }

    // ─── Subscriptions ───────────────────────────────────────────────────────────

    @GetMapping("/subscriptions")
    fun getSubscriptions(): ApiResponse<List<Map<String, Any?>>> {
        val plans = subscriptionPlanRepository.findAll()
        val dtos = plans.map { plan ->
            mapOf(
                "id" to plan.id.toString(),
                "name" to plan.name,
                "durationMonths" to plan.months,
                "price" to plan.priceUsd,
                "currency" to "USD",
                "features" to listOf("دسترسی به تمام امکانات", "تماس ویدیویی نامحدود", "بدون تبلیغات"),
                "lastUpdated" to plan.updatedAt.toString()
            )
        }
        return ApiResponse.success(dtos)
    }

    @PutMapping("/plans/{id}")
    fun updatePlan(@PathVariable id: UUID, @RequestBody request: Map<String, Any>): ApiResponse<String> {
        val plan = subscriptionPlanRepository.findById(id).orElseThrow { RuntimeException("Plan not found") }
        val newPrice = request["price"]?.toString()?.toDoubleOrNull() ?: throw RuntimeException("Invalid price")
        plan.priceUsd = newPrice
        subscriptionPlanRepository.save(plan)
        return ApiResponse.success("Plan updated successfully")
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private fun User.toAdminDto() = AdminUserDto(
        id = this.id.toString(),
        username = this.username,
        email = this.email,
        role = this.role?.name ?: "USER",
        status = if (this.isBanned) "BLOCKED" else "ACTIVE",
        subscriptionType = if (this.isPremium) "PREMIUM" else "FREE",
        isPremium = this.isPremium,
        premiumUntil = this.premiumUntil,
        registrationDate = this.createdAt,
        lastLogin = this.updatedAt,
        ipAddress = null
    )
}
