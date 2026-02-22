package com.iliyadev.minichat.api.controllers

import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal
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

@Controller
class ChatController(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    @MessageMapping("/chat/{matchId}")
    fun handleChatMessage(
        @DestinationVariable matchId: String,
        @Payload request: ChatMessageRequest,
        principal: Principal
    ) {
        logger.info("RECEIVED chat message for match $matchId from ${principal.name}: ${request.message}")
        
        val response = ChatMessageResponse(
            sender = principal.name,
            message = request.message,
            mediaUrl = request.mediaUrl,
            mediaType = request.mediaType
        )
        
        // Broadcast to both users in the match topic
        messagingTemplate.convertAndSend("/topic/chat/$matchId", response)
        logger.info("BROADCASTED chat message to /topic/chat/$matchId")
    }
}
