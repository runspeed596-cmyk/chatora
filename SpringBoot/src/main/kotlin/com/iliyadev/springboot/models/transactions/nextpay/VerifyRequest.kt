package net.holosen.onlineshop.models.transactions.nextpay

data class VerifyRequest(
    val apiKey: String,
    val transId: String,
    val amount: Int
)