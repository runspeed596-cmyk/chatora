package com.iliyadev.minichat.domain.usecases.auth

import com.iliyadev.minichat.api.dtos.AuthResponse
import com.iliyadev.minichat.api.dtos.GoogleLoginRequest
import com.iliyadev.minichat.core.exception.*
import com.iliyadev.minichat.core.security.GoogleAuthService
import com.iliyadev.minichat.core.security.JwtService
import com.iliyadev.minichat.domain.entities.User
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class GoogleLoginUseCase(
    private val userRepository: UserRepository,
    private val googleAuthService: GoogleAuthService,
    private val jwtService: JwtService,
    private val geoIPService: com.iliyadev.minichat.domain.services.GeoIPService
) {
    @Transactional
    fun execute(request: GoogleLoginRequest, ipAddress: String? = null): AuthResponse {
        val googleToken = googleAuthService.verifyToken(request.idToken)
            ?: throw InvalidCredentialsException("Invalid Google login. Please try again.")

        val payload = googleToken.payload
        val googleId = payload.subject
        val email = payload.email
        
        // Use email prefix as username (e.g., amooozeshbebin@gmail.com -> amooozeshbebin)
        val extractedUsername = email.substringBefore("@")
        val name = payload["name"] as? String ?: extractedUsername

        val detectedCountry = geoIPService.getCountryCode(ipAddress) ?: "US"

        var user = userRepository.findByGoogleId(googleId)
            .or { userRepository.findByEmail(email) }
            .orElseGet {
                val newUser = User(
                    username = name,
                    email = email,
                    googleId = googleId,
                    deviceId = request.deviceId,
                    countryCode = request.countryCode ?: detectedCountry,
                    languageCode = request.languageCode ?: "en"
                )
                userRepository.save(newUser)
            }

        // Update country from IP if detected and existing is default
        if (user.countryCode == null || user.countryCode == "US") {
            if (detectedCountry != "US") {
                user.countryCode = detectedCountry
            }
        }

        // Link Google ID if found by email
        if (user.googleId == null) {
            user.googleId = googleId
            userRepository.save(user)
        }
        
        // Update device ID
        if (user.deviceId != request.deviceId) {
            user.deviceId = request.deviceId
            userRepository.save(user)
        }

        if (user.isBanned) {
            throw BannedException(user.banReason)
        }

        val userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.username)
            .password("")
            .authorities("USER")
            .build()

        return AuthResponse(
            accessToken = jwtService.generateToken(userDetails),
            refreshToken = jwtService.generateRefreshToken(userDetails),
            userId = user.id.toString(),
            username = user.username,
            tempUsername = false
        )
    }
}
