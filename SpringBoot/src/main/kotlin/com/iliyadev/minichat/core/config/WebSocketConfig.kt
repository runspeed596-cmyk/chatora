package com.iliyadev.minichat.core.config

import com.iliyadev.minichat.core.security.JwtService
import com.iliyadev.minichat.core.security.CustomUserDetailsService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class WebSocketConfig(
    private val jwtService: JwtService,
    private val userDetailsService: CustomUserDetailsService
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .addInterceptors(IpHandshakeInterceptor())
            .withSockJS()
        
        registry.addEndpoint("/ws-native")
            .setAllowedOriginPatterns("*")
            .addInterceptors(IpHandshakeInterceptor())
    }

    class IpHandshakeInterceptor : HandshakeInterceptor {
        private val logger = LoggerFactory.getLogger(IpHandshakeInterceptor::class.java)

        override fun beforeHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            attributes: MutableMap<String, Any>
        ): Boolean {
                // Professional IP Extraction (Fulfills Point 1 of Fix Plan)
                val headers = request.headers
                val ip = headers.getFirst("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                    ?: headers.getFirst("X-Real-IP")
                    ?: request.remoteAddress.hostString
                
                attributes["IP_ADDRESS"] = ip
                logger.debug("Captured Handshake IP: $ip")
            return true
        }

        override fun afterHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            exception: Exception?
        ) {}
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                
                if (StompCommand.CONNECT == accessor?.command) {
                    // Extract IP for GeoIP (Requirement Point 1)
                    val ip = accessor.getFirstNativeHeader("X-Forwarded-For") ?: "127.0.0.1"
                    accessor.sessionAttributes?.put("IP_ADDRESS", ip)

                    val authHeader = accessor.getFirstNativeHeader("Authorization")
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        val token = authHeader.substring(7)
                        try {
                            val username = jwtService.extractUsername(token)
                            val userDetails = userDetailsService.loadUserByUsername(username)
                            if (jwtService.isTokenValid(token, userDetails)) {
                                val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                                accessor.user = auth
                                // logger.debug("WS Auth Success: $username") 
                            } else {
                                // logger.warn("WS Auth Failed: Invalid Token")
                            }
                        } catch (e: Exception) {
                            // Invalid token
                            org.slf4j.LoggerFactory.getLogger(WebSocketConfig::class.java).error("WS Auth Exception", e)
                        }
                    }
                }
                return message
            }
        })
    }
}
