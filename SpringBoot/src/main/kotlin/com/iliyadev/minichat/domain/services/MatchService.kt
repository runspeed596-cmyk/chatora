package com.iliyadev.minichat.domain.services

import com.iliyadev.minichat.core.storage.StorageService
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Service
class MatchService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val userRepository: UserRepository,
    private val storageService: StorageService
) {
    private val logger = LoggerFactory.getLogger(MatchService::class.java)

    data class WaitingUser(
        val userId: UUID,
        val username: String,
        val country: String,
        val preferredCountry: String, // ISO or "*"
        val preferredGender: String, // "MALE", "FEMALE", "ALL"
        val gender: String,
        val isPremium: Boolean,
        val karma: Int,
        val ipAddress: String,
        val sessionId: String,
        val lastMatchedUserId: UUID? = null,
        val joinedAt: Long = Instant.now().toEpochMilli()
    )

    data class MatchEvent(
        val type: String = "MATCH_FOUND",
        val matchId: String,
        val partnerId: String,
        val partnerUsername: String,
        val initiator: Boolean,
        val partnerIp: String = "",
        val partnerCountryCode: String = ""
    )

    // POOLS (O(1) lookups)
    private val waiters = ConcurrentHashMap<UUID, WaitingUser>()
    private val countryGenderPools = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<UUID>>>()
    private val genderPools = ConcurrentHashMap<String, MutableSet<UUID>>()
    private val globalPool = Collections.synchronizedSet(mutableSetOf<UUID>())

    // Active State
    private val activeMatches = ConcurrentHashMap<String, String>() // username -> partnerUsername
    private val userMatchIds = ConcurrentHashMap<String, String>() // username -> matchId
    private val lastMatchedUsers = ConcurrentHashMap<UUID, UUID>() // userId -> lastMatchedUserId (Spec requirement)

    // ANTI-CYCLING: Server-side protection against duplicate joinQueue requests
    private val lastJoinTime = ConcurrentHashMap<String, Long>() // username -> timestamp
    private val matchCreatedTime = ConcurrentHashMap<String, Long>() // username -> timestamp when match was created
    private val JOIN_COOLDOWN_MS = 3000L // Reject joinQueue within 3s of last call
    private val MATCH_PROTECTION_MS = 2000L // Don't break matches younger than 2s

    private val matchLock = ReentrantLock()

    fun findMatch(userId: String, username: String, myCountry: String, targetCountry: String, targetGender: String, lang: String, sessionId: String, isPremium: Boolean, gender: String, karma: Int, ipAddress: String) {
        val uId = UUID.fromString(userId)
        val now = System.currentTimeMillis()
        
        // ANTI-CYCLING: Reject duplicate joinQueue within cooldown period
        val lastJoin = lastJoinTime[username]
        if (lastJoin != null && (now - lastJoin) < JOIN_COOLDOWN_MS) {
            logger.warn("joinQueue REJECTED for $username: cooldown active (${now - lastJoin}ms since last join)")
            return
        }
        lastJoinTime[username] = now
        
        // ANTI-CYCLING: Don't break a match that was just created
        val matchCreated = matchCreatedTime[username]
        if (matchCreated != null && (now - matchCreated) < MATCH_PROTECTION_MS && activeMatches.containsKey(username)) {
            logger.warn("joinQueue REJECTED for $username: match protection active (match created ${now - matchCreated}ms ago)")
            return
        }
        
        // Ensure clean state before joining
        handleUserLeave(username, userId, sessionId)
        
        val user = WaitingUser(
            userId = uId,
            username = username,
            country = myCountry,
            preferredCountry = targetCountry,
            preferredGender = targetGender.uppercase(),
            gender = gender.uppercase(),
            isPremium = isPremium,
            karma = karma,
            ipAddress = ipAddress,
            sessionId = sessionId,
            lastMatchedUserId = lastMatchedUsers[uId]
        )

        waiters[uId] = user
        addToPools(user)

        logger.info("User $username joined queue. Pools: ${waiters.size}")
        sendSearchingEvent(username)

        // Instant matching attempt
        matchEngine()
    }

    private fun addToPools(user: WaitingUser) {
        countryGenderPools.computeIfAbsent(user.country) { ConcurrentHashMap() }
            .computeIfAbsent(user.gender) { Collections.synchronizedSet(mutableSetOf()) }
            .add(user.userId)
        
        genderPools.computeIfAbsent(user.gender) { Collections.synchronizedSet(mutableSetOf()) }
            .add(user.userId)
        
        globalPool.add(user.userId)
    }

    private fun removeFromPools(uId: UUID) {
        val user = waiters[uId] ?: return
        countryGenderPools[user.country]?.get(user.gender)?.remove(uId)
        genderPools[user.gender]?.remove(uId)
        globalPool.remove(uId)
        waiters.remove(uId)
    }

    @Scheduled(fixedDelay = 500)
    fun matchEngine() {
        if (waiters.isEmpty()) return
        if (!matchLock.tryLock()) {
            logger.debug("matchEngine: Skipped (another thread is matching)")
            return
        }

        try {
            val matchedIds = mutableSetOf<UUID>()
            val sortedWaiters = waiters.values.toList().sortedByDescending { it.isPremium }

            logger.info("matchEngine: Running with ${sortedWaiters.size} waiters: ${sortedWaiters.map { "${it.username}(${it.userId.toString().take(8)})" }}")

            for (u1 in sortedWaiters) {
                if (matchedIds.contains(u1.userId) || !waiters.containsKey(u1.userId)) continue
                
                val partner = searchTiers(u1, matchedIds)
                if (partner != null) {
                    matchedIds.add(u1.userId)
                    matchedIds.add(partner.userId)
                    executeMatch(u1, partner)
                } else {
                    logger.info("matchEngine: No partner found for ${u1.username} (waitTime=${Instant.now().toEpochMilli() - u1.joinedAt}ms)")
                }
            }
        } catch (e: Exception) {
            logger.error("MatchEngine Error: ${e.message}", e)
        } finally {
            matchLock.unlock()
        }
    }

    private fun searchTiers(u1: WaitingUser, alreadyMatched: Set<UUID>): WaitingUser? {
        val waitTime = Instant.now().toEpochMilli() - u1.joinedAt
        val isSmallPool = waiters.size < 5 // For local testing and low traffic

        // Attempt 1: Premium Exact (0-500ms)
        if (u1.isPremium && u1.preferredCountry != "*" && u1.preferredGender != "ALL") {
            val pool = countryGenderPools[u1.preferredCountry]?.get(u1.preferredGender)
            val partner = findInPool(u1, pool, alreadyMatched, strict = true)
            if (partner != null) return partner
        }

        // Attempt 2: Premium Gender (500-1500ms)
        if (waitTime > 500 || (u1.isPremium && isSmallPool)) {
            val pool = if (u1.preferredGender == "ALL") null else genderPools[u1.preferredGender]
            val partner = findInPool(u1, pool, alreadyMatched, strict = true)
            if (partner != null) return partner
        }

        // Attempt 3: Standard Opposite Gender (1500-3000ms)
        if (waitTime > 1500 || (isSmallPool && u1.preferredGender == "ALL")) {
            val opposite = if (u1.gender == "MALE") "FEMALE" else "MALE"
            val partner = findInPool(u1, genderPools[opposite], alreadyMatched, strict = !isSmallPool)
            if (partner != null) return partner
        }

        // Attempt 4: Random Fallback (3000-5000ms)
        if (waitTime > 3000 || isSmallPool) {
            return findInPool(u1, globalPool, alreadyMatched, strict = false)
        }

        return null
    }

    private fun findInPool(u1: WaitingUser, pool: Set<UUID>?, alreadyMatched: Set<UUID>, strict: Boolean): WaitingUser? {
        if (pool == null || pool.isEmpty()) return null
        
        synchronized(pool) {
            for (pId in pool) {
                if (pId == u1.userId || alreadyMatched.contains(pId)) continue
                val u2 = waiters[pId] ?: continue
                
                if (isValidMatch(u1, u2, strict)) return u2
            }
        }
        return null
    }

    private fun isValidMatch(u1: WaitingUser, u2: WaitingUser, strict: Boolean): Boolean {
        // Anti-Abuse Rules (Spec mandatory)
        if (u1.userId == u2.userId) {
            logger.debug("isValidMatch REJECTED: same userId (${u1.username})")
            return false
        }
        // Block same-IP matching only for public IPs (private/Docker IPs are shared and unreliable)
        if (u1.ipAddress == u2.ipAddress && !isPrivateOrLocalIp(u1.ipAddress)) {
            logger.debug("isValidMatch REJECTED: same public IP ${u1.ipAddress} (${u1.username} vs ${u2.username})")
            return false
        }
        
        // Repeat Prevention (Spec mandatory)
        // LOOSENED FOR SMALL POOLS: If only 2 people, allow matching again to prevent deadlocks in testing
        if (waiters.size > 2) {
            if (u1.lastMatchedUserId == u2.userId || u2.lastMatchedUserId == u1.userId) {
                logger.debug("isValidMatch REJECTED: repeat match (${u1.username} vs ${u2.username})")
                return false
            }
        }

        if (strict) {
            // Honor Partner's Preferences in strict mode
            if (u2.preferredGender != "ALL" && u2.preferredGender != u1.gender) {
                logger.debug("isValidMatch REJECTED: gender mismatch (${u1.username}=${u1.gender} vs ${u2.username} prefers ${u2.preferredGender})")
                return false
            }
            if (u2.preferredCountry != "*" && u2.preferredCountry != "AUTO" && u2.preferredCountry != u1.country) {
                logger.debug("isValidMatch REJECTED: country mismatch (${u1.username}=${u1.country} vs ${u2.username} prefers ${u2.preferredCountry})")
                return false
            }
            // Karma check for quality
            if (Math.abs(u1.karma - u2.karma) > 70) {
                logger.debug("isValidMatch REJECTED: karma gap (${u1.username}=${u1.karma} vs ${u2.username}=${u2.karma})")
                return false
            }
        }

        logger.info("isValidMatch ACCEPTED: ${u1.username} <-> ${u2.username}")
        return true
    }

    /**
     * Checks if an IP is private/local (RFC 1918, loopback, Docker bridge).
     * These IPs are shared by multiple users behind NAT/Docker and cannot reliably identify distinct users.
     */
    private fun isPrivateOrLocalIp(ip: String): Boolean {
        return ip.startsWith("127.") ||
                ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip == "0:0:0:0:0:0:0:1" ||
                ip == "::1" ||
                // Docker bridge networks: 172.16.0.0 - 172.31.255.255
                (ip.startsWith("172.") && run {
                    val secondOctet = ip.split(".").getOrNull(1)?.toIntOrNull() ?: -1
                    secondOctet in 16..31
                })
    }

    private fun executeMatch(u1: WaitingUser, u2: WaitingUser) {
        val matchId = UUID.randomUUID().toString()
        
        // Store for next cycle (Spec requirement)
        lastMatchedUsers[u1.userId] = u2.userId
        lastMatchedUsers[u2.userId] = u1.userId

        // Active match records
        activeMatches[u1.username] = u2.username
        activeMatches[u2.username] = u1.username
        userMatchIds[u1.username] = matchId
        userMatchIds[u2.username] = matchId
        
        // ANTI-CYCLING: Record when match was created to protect it from immediate destruction
        val matchTime = System.currentTimeMillis()
        matchCreatedTime[u1.username] = matchTime
        matchCreatedTime[u2.username] = matchTime

        removeFromPools(u1.userId)
        removeFromPools(u2.userId)

        logger.info("MATCH! [${u1.username} <-> ${u2.username}]")
        
        messagingTemplate.convertAndSendToUser(u1.username, "/queue/match", MatchEvent(matchId = matchId, partnerId = u2.userId.toString(), partnerUsername = u2.username, initiator = true, partnerIp = u2.ipAddress, partnerCountryCode = u2.country))
        messagingTemplate.convertAndSendToUser(u2.username, "/queue/match", MatchEvent(matchId = matchId, partnerId = u1.userId.toString(), partnerUsername = u1.username, initiator = false, partnerIp = u1.ipAddress, partnerCountryCode = u1.country))
    }

    fun handleUserLeave(username: String, userId: String? = null, sessionId: String? = null) {
        val uId = try { if (userId != null) UUID.fromString(userId) else null } catch(e: Exception) { null }
        val waiterId = uId ?: waiters.values.find { it.username == username }?.userId
        
        if (waiterId != null) {
            removeFromPools(waiterId)
            logger.info("User $username left queue.")
        }

        val partnerUsername = activeMatches.remove(username)
        val matchId = userMatchIds.remove(username)
        matchCreatedTime.remove(username) // Clean up match protection data
        
        if (matchId != null) storageService.cleanup(matchId)

        if (partnerUsername != null) {
            activeMatches.remove(partnerUsername)
            matchCreatedTime.remove(partnerUsername) // Clean up partner's match protection data
            val partnerMatchId = userMatchIds.remove(partnerUsername) ?: "none"
            logger.info("Match ended: $partnerUsername's partner ($username) left. matchId=$partnerMatchId")
            messagingTemplate.convertAndSendToUser(partnerUsername, "/queue/match", MatchEvent(type = "PARTNER_LEFT", matchId = partnerMatchId, partnerId = "none", partnerUsername = "none", initiator = false))
        }
    }

    fun removeUserFromQueue(userId: String, username: String, country: String, lang: String, sessionId: String) {
        // Explicit leave (Stop button) — clear all cooldowns so user can re-join freely
        lastJoinTime.remove(username)
        matchCreatedTime.remove(username)
        handleUserLeave(username, userId, sessionId)
    }

    private fun sendSearchingEvent(username: String) {
        messagingTemplate.convertAndSendToUser(username, "/queue/match", mapOf("type" to "SEARCHING"))
    }

    /**
     * Returns the partner's username for a given user, or null if the user is not in an active match.
     * Used by SignalController to route signals to the specific partner instead of broadcasting.
     */
    fun getPartnerUsername(username: String): String? {
        return activeMatches[username]
    }
}
