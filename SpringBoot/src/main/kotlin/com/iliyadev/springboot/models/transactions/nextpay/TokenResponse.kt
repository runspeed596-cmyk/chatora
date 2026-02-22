package net.holosen.onlineshop.models.transactions.nextpay

data class TokenResponse(
    val code: Int,
    val trans_id: String,
    val amount: Int
)