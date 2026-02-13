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
                // Professional IP Extraction with full debug logging
                val headers = request.headers
                
                val xForwardedFor = headers.getFirst("X-Forwarded-For")
                val xRealIp = headers.getFirst("X-Real-IP")
                val remoteAddr = request.remoteAddress?.hostString
                
                // Also try Servlet-level access for edge cases
                val servletRemoteAddr = if (request is ServletServerHttpRequest) {
                    request.servletRequest.remoteAddr
                } else null
                
                logger.info("=== WS Handshake IP Debug ===")
                logger.info("X-Forwarded-For: $xForwardedFor")
                logger.info("X-Real-IP: $xRealIp")
                logger.info("remoteAddress.hostString: $remoteAddr")
                logger.info("servletRequest.remoteAddr: $servletRemoteAddr")
                
                val ip = xForwardedFor?.split(",")?.firstOrNull()?.trim()
                    ?: xRealIp
                    ?: servletRemoteAddr
                    ?: remoteAddr
                    ?: "127.0.0.1"
                
                attributes["IP_ADDRESS"] = ip
                logger.info("Final Captured IP: $ip")
                logger.info("=== End WS Handshake IP Debug ===")
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
                    // ONLY set if not already present from HandshakeInterceptor
                    if (accessor.sessionAttributes?.containsKey("IP_ADDRESS") != true) {
                        val ip = accessor.getFirstNativeHeader("X-Forwarded-For") ?: "127.0.0.1"
                        accessor.sessionAttributes?.put("IP_ADDRESS", ip)
                    }

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
