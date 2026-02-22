package com.iliyadev.minichat.domain.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Real GeoIP service using ip-api.com (free tier: 45 req/min, HTTP only).
 *
 * Purpose: Resolve an IP address to its ISO 3166-1 alpha-2 country code.
 * Dependencies: java.net.http.HttpClient (JDK 11+), Jackson ObjectMapper.
 * Inputs: IP address string.
 * Outputs: Two-letter country code (e.g., "US", "DE", "IR").
 * Failure Modes: Network timeout, rate limit exceeded, invalid IP → returns "US" fallback.
 */
@Service
class GeoIPService {

    private val logger = LoggerFactory.getLogger(GeoIPService::class.java)
    private val objectMapper = ObjectMapper()

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()

    // --- In-Memory Cache ---
    // Key: IP address, Value: CachedEntry(countryCode, timestamp)
    private data class CachedEntry(val countryCode: String, val cachedAt: Long)

    private val cache = ConcurrentHashMap<String, CachedEntry>()
    private val cacheTtlMs: Long = 30 * 60 * 1000L // 30 minutes

    // ip-api.com response DTO
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class IpApiResponse(
        val status: String = "",
        val countryCode: String = "",
        val query: String = ""
    )

    // Private/local IP ranges
    private val privateIpPrefixes = listOf(
        "127.", "10.", "192.168.", "0:0:0:0:0:0:0:1", "::1"
    )

    /**
     * Resolves an IP address to a country code.
     *
     * @param ipAddress The IP address to look up.
     * @return ISO 3166-1 alpha-2 country code, or "US" as fallback.
     */
    fun getCountryCode(ipAddress: String?): String {
        if (ipAddress.isNullOrBlank()) {
            logger.warn("GeoIP: Received null/blank IP, returning default US")
            return "US"
        }

        // Private/local IPs cannot be resolved by external GeoIP services
        if (isPrivateIp(ipAddress)) {
            logger.info("GeoIP: Private/local IP detected: $ipAddress — returning US fallback")
            return "US"
        }

        // Check cache first (fast path — no blocking I/O)
        val cached = cache[ipAddress]
        if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < cacheTtlMs) {
            logger.debug("GeoIP: Cache hit for $ipAddress -> ${cached.countryCode}")
            return cached.countryCode
        }

        // Cache miss → call ip-api.com
        return try {
            val countryCode = lookupFromApi(ipAddress)
            cache[ipAddress] = CachedEntry(countryCode, System.currentTimeMillis())
            logger.info("GeoIP: Resolved $ipAddress -> $countryCode")
            countryCode
        } catch (e: Exception) {
            logger.error("GeoIP: Failed to resolve $ipAddress — ${e.message}")
            // Return stale cache entry if available, otherwise fallback
            cached?.countryCode ?: "US"
        }
    }

    /**
     * Performs the actual HTTP call to ip-api.com.
     * Uses HTTP (not HTTPS) as required by ip-api.com free tier.
     */
    private fun lookupFromApi(ipAddress: String): String {
        val url = "http://ip-api.com/json/$ipAddress?fields=status,countryCode,query"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(3))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw RuntimeException("ip-api.com returned HTTP ${response.statusCode()}")
        }

        val apiResponse = objectMapper.readValue(response.body(), IpApiResponse::class.java)

        if (apiResponse.status != "success") {
            throw RuntimeException("ip-api.com lookup failed for $ipAddress (status: ${apiResponse.status})")
        }

        val code = apiResponse.countryCode
        if (code.isBlank()) {
            throw RuntimeException("ip-api.com returned empty countryCode for $ipAddress")
        }

        return code
    }

    private fun isPrivateIp(ip: String): Boolean {
        return privateIpPrefixes.any { ip.startsWith(it) } ||
                // 172.16.0.0 - 172.31.255.255
                (ip.startsWith("172.") && run {
                    val secondOctet = ip.split(".").getOrNull(1)?.toIntOrNull() ?: -1
                    secondOctet in 16..31
                })
    }
}
