package com.iliyadev.minichat.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "subscriptions")
data class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan", referencedColumnName = "name", nullable = false)
    val plan: SubscriptionPlan,

    @Column(nullable = false)
    val startDate: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var endDate: LocalDateTime,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
