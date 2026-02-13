package com.iliyadev.minichat.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "subscription_plans")
data class SubscriptionPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    var name: String = "",

    var months: Int = 1,

    var priceUsd: Double = 0.0,

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
