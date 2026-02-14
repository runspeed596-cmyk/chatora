package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.domain.services.MatchService
import com.iliyadev.minichat.domain.services.GeoIPService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller
import java.security.Principal

data class JoinQueueRequest(
    val myCountry: String = "US",
    val targetCountry: String = "*",
    val targetGender: String = "ALL", // ALL, MALE, FEMALE
    val lang: String = "en"
)

@Controller
class MatchController(
    private val matchService: MatchService,
    private val geoIPService: GeoIPService,
    private val userRepository: com.iliyadev.minichat.domain.repositories.UserRepository
) {
    
    // Simple In-Memory Cache to prevent DB hits on every "Next Match" click
    // Key: Username, Value: CachedUser(id, isPremium, gender)
    private val userCache = java.util.concurrent.ConcurrentHashMap<String, CachedUser>()
    data class CachedUser(val id: String, val isPremium: Boolean, val username: String, val gender: String, val karma: Int)

    fun clearCacheForUser(username: String) {
        userCache.remove(username)
    }

    private val logger = LoggerFactory.getLogger(MatchController::class.java)

    @MessageMapping("/match/join")
    fun joinQueue(
        @Payload request: JoinQueueRequest, 
        authentication: java.util.Optional<org.springframework.security.core.Authentication>,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val auth = authentication.orElse(null)
        logger.info("DEBUG: joinQueue reached! Request: $request, Auth: $auth")
        
        if (auth == null || !auth.isAuthenticated) {
            logger.error("Match join failed: Authentication is null or not authenticated")
            return
        }
        val username = auth.name

        // Professional Implementation: Use real IP from Stomp Headers
        val ipAddress = headerAccessor.getSessionAttributes()?.get("IP_ADDRESS") as? String ?: "127.0.0.1"
        
        val myCountry = if (request.myCountry == "AUTO") {
             geoIPService.getCountryCode(ipAddress) ?: "US"
        } else {
             request.myCountry
        }

        // OPTIMIZATION: CP-100x Speed - Check Cache First
        val cachedUser = userCache[username]
        
        val (userId, isPremium, cachedUsername, gender, karma) = if (cachedUser != null) {
            cachedUser
        } else {
            // Fallback to DB
            val user = userRepository.findByUsername(username).orElseThrow {
                IllegalArgumentException("User found in Principal but not in DB: $username")
            }
            val idStr = user.id.toString()
            val genderStr = user.gender.name // Assuming Gender enum
            val newCache = CachedUser(idStr, user.isPremium, user.username, genderStr, user.karma)
            userCache[username] = newCache
            newCache
        }

        matchService.findMatch(
            userId = userId,
            username = cachedUsername,
            myCountry = myCountry,
            targetCountry = request.targetCountry,
            targetGender = request.targetGender,
            lang = request.lang,
            sessionId = headerAccessor.sessionId ?: "unknown",
            isPremium = isPremium,
            gender = gender,
            karma = karma,
            ipAddress = ipAddress
        )
    }

    @MessageMapping("/match/leave")
    fun leaveQueue(
        @Payload request: JoinQueueRequest, 
        principalOpt: java.util.Optional<Principal>,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val principal = principalOpt.orElse(null)
        if (principal == null) return

        // Attempt to find user, but don't crash on leave if not found (maybe already deleted)
        val user = userRepository.findByUsername(principal.name).orElse(null)
        val userId = user?.id?.toString() ?: principal.name // Fallback if needed, though likely UUID required

        matchService.removeUserFromQueue(
            userId = userId, 
            username = principal.name, 
            country = request.myCountry, 
            lang = request.lang,
            sessionId = headerAccessor.sessionId ?: "unknown"
        )
    }
}

@Controller
class SignalController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val matchService: MatchService
) {
    private val logger = LoggerFactory.getLogger(SignalController::class.java)

    @MessageMapping("/signal/{matchId}")
    fun handleSignal(
        @DestinationVariable matchId: String,
        @Payload signal: Map<String, Any>,
        principalOpt: java.util.Optional<Principal>
    ) {
        val principal = principalOpt.orElse(null) ?: return
        val sender = principal.name
        
        // Send to partner ONLY — prevents echo-back where each peer receives its own signals
        val partner = matchService.getPartnerUsername(sender)
        if (partner != null) {
            logger.debug("Signal for match $matchId from $sender -> $partner: ${signal["type"]}")
            messagingTemplate.convertAndSendToUser(partner, "/queue/call", signal)
        } else {
            logger.warn("Signal for match $matchId from $sender: No active partner found (match may have ended)")
        }
    }
}
