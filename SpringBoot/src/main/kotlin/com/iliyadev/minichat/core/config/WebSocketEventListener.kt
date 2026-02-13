package com.iliyadev.minichat.core.config

import com.iliyadev.minichat.domain.services.MatchService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(
    private val matchService: MatchService
) {
    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val username = event.user?.name
        val sessionId = event.sessionId
        if (username != null) {
            logger.info("User Disconnected: $username (Session: $sessionId). Cleaning up state.")
            matchService.handleUserLeave(username, null, sessionId)
        }
    }
}
