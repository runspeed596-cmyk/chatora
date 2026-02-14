package com.iliyadev.minichat.domain.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GeoIPService {
    private val logger = LoggerFactory.getLogger(GeoIPService::class.java)

    /**
     * MOCK GeoIP detection. 
     * In a production environment, this would use MaxMind, IP2Location, or a similar library.
     */
    fun getCountryCode(ipAddress: String?): String {
        if (ipAddress == null) return "US" // Default fallback
        
        logger.info("Detecting country for IP: $ipAddress")
        
        // Professional Mock: Tailored for user's Iranian development environment
        return when {
            ipAddress == "127.0.0.1" || ipAddress == "0:0:0:0:0:0:0:1" -> "IR"
            ipAddress.startsWith("185.") || ipAddress.startsWith("5.x") -> "IR"
            ipAddress.startsWith("91.") -> "DE"
            ipAddress.startsWith("104.") -> "US"
            else -> "IR" // Default to User's home country for best local testing feel
        }
    }
}
