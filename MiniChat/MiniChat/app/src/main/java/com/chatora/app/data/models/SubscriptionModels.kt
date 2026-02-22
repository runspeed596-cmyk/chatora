package com.chatora.app.data.models

import com.google.gson.annotations.SerializedName

data class SubscriptionPlanDto(
    val name: String,
    val months: Int,
    val priceUsd: Double
)

data class CreatePaymentRequestDto(
    val plan: String
)

data class CreatePaymentResponseDto(
    val paymentLink: String,
    val paymentId: String,
    val orderId: String
)

data class SubscriptionStatusDto(
    val isPremium: Boolean,
    val premiumUntil: String?, // ISO Date string
    val plan: String?
)

data class PaymentStatusDto(
    val status: String,
    val _id: String,
    val orderId: String
)
