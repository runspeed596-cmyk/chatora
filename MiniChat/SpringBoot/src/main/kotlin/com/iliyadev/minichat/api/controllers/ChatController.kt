package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.api.dtos.ChatMessageRequest
import com.iliyadev.minichat.api.dtos.ChatMessageResponse
import com.iliyadev.minichat.domain.services.MatchService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val matchService: MatchService
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
        
        // Store in buffer for admin monitoring
        matchService.addMessageToBuffer(matchId, response)
        
        // Broadcast to both users in the match topic
        messagingTemplate.convertAndSend("/topic/chat/$matchId", response)
        logger.info("BROADCASTED chat message to /topic/chat/$matchId")
    }
}
