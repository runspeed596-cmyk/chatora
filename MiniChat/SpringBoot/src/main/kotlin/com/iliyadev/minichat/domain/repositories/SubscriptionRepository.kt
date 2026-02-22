package com.iliyadev.minichat.domain.repositories

import com.iliyadev.minichat.domain.entities.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubscriptionRepository : JpaRepository<Subscription, UUID> {
    fun findByUserId(userId: UUID): List<Subscription>
    
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.isActive = true AND s.endDate > CURRENT_TIMESTAMP")
    fun findActiveSubscriptionByUserId(userId: UUID): Subscription?
}
