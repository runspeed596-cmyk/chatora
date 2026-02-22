package com.iliyadev.minichat.domain.repositories

import com.iliyadev.minichat.domain.entities.PaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, UUID> {
    fun findByOrderId(orderId: String): PaymentTransaction?
    fun findByExternalPaymentId(externalPaymentId: String): PaymentTransaction?
    fun countByStatus(status: com.iliyadev.minichat.domain.entities.PaymentStatus): Long

    @org.springframework.data.jpa.repository.Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.status = :status")
    fun sumAmountByStatus(status: com.iliyadev.minichat.domain.entities.PaymentStatus): Double?

    @org.springframework.data.jpa.repository.Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.status = :status AND pt.updatedAt > :date")
    fun sumAmountByStatusAndUpdatedAtAfter(
        status: com.iliyadev.minichat.domain.entities.PaymentStatus, 
        date: java.time.LocalDateTime
    ): Double?
}
