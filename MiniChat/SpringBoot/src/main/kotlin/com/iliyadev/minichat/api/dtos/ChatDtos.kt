package com.iliyadev.minichat.api.dtos

import java.time.Instant

data class ChatMessageRequest(
    val message: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)

data class ChatMessageResponse(
    val sender: String,
    val message: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val timestamp: Long = Instant.now().toEpochMilli()
)
