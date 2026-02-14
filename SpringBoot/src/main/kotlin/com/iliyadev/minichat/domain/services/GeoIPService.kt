package com.iliyadev.minichat.domain.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ConcurrentHashMap

/**
 * GeoIP detection service using ip-api.com (free, no API key, 45 req/min).
 *
 * Purpose: Resolve a client IP address to a country code + country name.
 * Inputs: IP address string (IPv4 or IPv6).
 * Outputs: CountryInfo(countryCode, countryName).
 * Dependencies: Spring RestTemplate, ip-api.com HTTP API.
 * Failure Modes: Network timeout → returns UNKNOWN. Private IP → returns UNKNOWN.
 */
@Service
class GeoIPService {
    private val logger = LoggerFactory.getLogger(GeoIPService::class.java)
    private val restTemplate = RestTemplate()

    data class CountryInfo(
        val countryCode: String,
        val countryName: String
    ) {
        companion object {
            val UNKNOWN = CountryInfo("UNKNOWN", "Unknown")
        }
    }

    // In-memory cache: IP → (CountryInfo, timestampMs)
    private data class CacheEntry(val info: CountryInfo, val cachedAt: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheTtlMs = 3600_000L // 1 hour TTL

    /**
     * Returns the ISO 2-letter country code for the given IP.
     * Backward-compatible method used by MatchController for myCountry detection.
     */
    fun getCountryCode(ipAddress: String?): String {
        if (ipAddress == null) return "US"
        return getCountryInfo(ipAddress).countryCode.let {
            if (it == "UNKNOWN") "US" else it
        }
    }

    /**
     * Returns full country info (code + name) for the given IP.
     * Used by MatchService.executeMatch to populate MatchEvent.
     */
    fun getCountryInfo(ipAddress: String): CountryInfo {
        // Handle private/loopback IPs — cannot be geo-located
        if (isPrivateOrLoopback(ipAddress)) {
            logger.debug("Private/loopback IP detected: $ipAddress — returning UNKNOWN")
            return CountryInfo.UNKNOWN
        }

        // Check cache
        val cached = cache[ipAddress]
        if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < cacheTtlMs) {
            logger.debug("GeoIP cache hit for $ipAddress: ${cached.info}")
            return cached.info
        }

        // Call ip-api.com
        return try {
            val url = "http://ip-api.com/json/$ipAddress?fields=status,countryCode,country"
            val response = restTemplate.getForObject(url, Map::class.java)

            if (response != null && response["status"] == "success") {
                val code = response["countryCode"] as? String ?: "UNKNOWN"
                val name = response["country"] as? String ?: "Unknown"
                val info = CountryInfo(code, name)

                // Cache the result
                cache[ipAddress] = CacheEntry(info, System.currentTimeMillis())
                logger.info("GeoIP resolved $ipAddress → $code ($name)")
                info
            } else {
                logger.warn("GeoIP lookup failed for $ipAddress: $response")
                CountryInfo.UNKNOWN
            }
        } catch (e: Exception) {
            logger.error("GeoIP lookup error for $ipAddress: ${e.message}")
            CountryInfo.UNKNOWN
        }
    }

    /**
     * Detect private, loopback, and link-local IP addresses.
     * These cannot be resolved by external GeoIP services.
     */
    private fun isPrivateOrLoopback(ip: String): Boolean {
        return ip == "127.0.0.1" ||
                ip.startsWith("127.") ||
                ip == "0:0:0:0:0:0:0:1" ||
                ip == "::1" ||
                ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
                ip.startsWith("172.2") || ip.startsWith("172.3") ||
                ip.startsWith("169.254.") || // Link-local
                ip == "0.0.0.0" ||
                ip.isBlank()
    }
}
