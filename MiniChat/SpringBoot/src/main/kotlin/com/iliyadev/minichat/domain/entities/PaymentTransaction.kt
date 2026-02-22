package com.iliyadev.minichat.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payment_transactions")
data class PaymentTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(unique = true)
    val externalPaymentId: String? = null, // 1xgate _id

    @Column(unique = true, nullable = false)
    val orderId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.CREATED,

    @Column(nullable = false)
    val amount: Double,

    @Column(nullable = false)
    val currency: String = "USDT",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan", referencedColumnName = "name", nullable = false)
    val plan: SubscriptionPlan,

    @Column(columnDefinition = "TEXT")
    var webhookPayload: String? = null,

    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class PaymentStatus {
    CREATED, PENDING, SUCCESS, FAILED, EXPIRED, PARTIALLY_PAID, ERROR
}
