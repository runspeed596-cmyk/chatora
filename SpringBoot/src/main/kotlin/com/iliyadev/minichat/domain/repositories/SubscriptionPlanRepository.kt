package com.iliyadev.minichat.domain.repositories

import com.iliyadev.minichat.domain.entities.SubscriptionPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubscriptionPlanRepository : JpaRepository<SubscriptionPlan, UUID> {
    fun findByName(name: String): SubscriptionPlan?
}
