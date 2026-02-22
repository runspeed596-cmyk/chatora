package com.iliyadev.minichat.api.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import com.iliyadev.minichat.domain.entities.PaymentStatus
import java.time.LocalDateTime

data class CreatePaymentRequest(
    val plan: String
)

data class CreatePaymentResponse(
    val paymentLink: String,
    val paymentId: String,
    val orderId: String
)

data class SubscriptionStatusResponse(
    val isPremium: Boolean,
    val premiumUntil: LocalDateTime?,
    val plan: String?
)

data class PlanDto(
    val name: String,
    val months: Int,
    val priceUsd: Double
)

data class XGateCreatePaymentRequest(
    val currency: String,
    val network: String,
    val amount: Double,
    val orderId: String,
    val callbacks: XGateCallbacks
)

data class XGateCallbacks(
    val success: String,
    val failed: String
)

data class XGatePaymentResponse(
    val data: XGatePaymentData
)

data class XGatePaymentData(
    val _id: String,
    val link: String,
    val status: String,
    val orderId: String
)

data class XGateWebhookPayload(
    val _id: String,
    val type: String,
    val status: String,
    val amount: Double,
    val currency: String,
    val network: String
)
