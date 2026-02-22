package com.iliyadev.minichat.domain.services

import com.iliyadev.minichat.domain.entities.Subscription
import com.iliyadev.minichat.domain.entities.SubscriptionPlan
import com.iliyadev.minichat.domain.repositories.SubscriptionRepository
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository
) {
    fun getActiveSubscription(userId: UUID): Subscription? {
        return subscriptionRepository.findActiveSubscriptionByUserId(userId)
    }

    fun isPremiumUser(userId: UUID): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        if (user.isPremium && user.premiumUntil != null && user.premiumUntil!!.isAfter(LocalDateTime.now())) {
            return true
        }
        // Fallback check if flag is out of sync
        val activeSub = getActiveSubscription(userId)
        if (activeSub != null) {
            updateUserPremiumStatus(userId, activeSub.endDate)
            return true
        }
        return false
    }

    @Transactional
    fun activateSubscription(userId: UUID, plan: SubscriptionPlan) {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        
        val now = LocalDateTime.now()
        val currentSub = getActiveSubscription(userId)
        
        val startDate = if (currentSub != null && currentSub.endDate.isAfter(now)) {
            currentSub.endDate
        } else {
            now
        }
        
        val endDate = startDate.plusMonths(plan.months.toLong())
        
        val subscription = Subscription(
            user = user,
            plan = plan,
            startDate = startDate,
            endDate = endDate,
            isActive = true
        )
        
        // Deactivate old active subscriptions
        currentSub?.let {
            it.isActive = false
            subscriptionRepository.save(it)
        }
        
        subscriptionRepository.save(subscription)
        updateUserPremiumStatus(userId, endDate)
    }

    private fun updateUserPremiumStatus(userId: UUID, endDate: LocalDateTime) {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        user.isPremium = true
        user.premiumUntil = endDate
        userRepository.save(user)
    }
}
