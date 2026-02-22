package net.holosen.onlineshop.models.transactions.nextpay

data class TokenRequest(
    val api_key: String,
    val order_id: String,
    val amount: Int,
    val callback_uri: String,
    val currency: String = "IRT",
    val customer_phone: String,
    val auto_verify: String = "no"
)