package com.iliyadev.minichat.domain.usecases.auth

import com.iliyadev.minichat.api.dtos.AuthResponse
import com.iliyadev.minichat.api.dtos.LoginRequest
import com.iliyadev.minichat.core.exception.*
import com.iliyadev.minichat.core.security.JwtService
import com.iliyadev.minichat.domain.entities.User
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun execute(request: LoginRequest, ipAddress: String? = null): AuthResponse {
        // 1. Check for username/password login (Admin Panel)
        if (!request.username.isNullOrBlank() && !request.password.isNullOrBlank()) {
            val user = userRepository.findByUsername(request.username)
                .orElseThrow { InvalidCredentialsException("Invalid username or password") }
            
            if (user.password == null || !passwordEncoder.matches(request.password, user.password)) {
                throw InvalidCredentialsException("Invalid username or password")
            }

            if (user.isBanned) {
                throw BannedException(user.banReason)
            }

            return generateAuthResponse(user)
        }

        // 2. Fallback to Device-based Guest Login
        val deviceId = request.deviceId ?: throw ApiException("INVALID_REQUEST", "DeviceId is required for guest login")
        
        var user = userRepository.findByDeviceId(deviceId)
            .orElseGet {
                val newUser = User(
                    username = "User_" + UUID.randomUUID().toString().substring(0, 8),
                    deviceId = deviceId,
                    countryCode = detectCountryFromIp(ipAddress) ?: request.countryCode ?: "US",
                    languageCode = request.languageCode ?: "en"
                )
                userRepository.save(newUser)
            }

        if (user.countryCode != request.countryCode || user.languageCode != request.languageCode) {
            user.countryCode = request.countryCode
            user.languageCode = request.languageCode
            userRepository.save(user)
        }

        if (user.isBanned) {
            throw BannedException(user.banReason)
        }

        return generateAuthResponse(user)
    }

    private fun generateAuthResponse(user: User): AuthResponse {
        val userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.username)
            .password("")
            .authorities(user.role.name)
            .build()

        return AuthResponse(
            accessToken = jwtService.generateToken(userDetails),
            refreshToken = jwtService.generateRefreshToken(userDetails),
            userId = user.id.toString(),
            username = user.username,
            tempUsername = user.password == null
        )
    }
    private fun detectCountryFromIp(ip: String?): String? {
        if (ip == null || ip == "127.0.0.1" || ip == "0:0:0:0:0:0:0:1") return "IR" // Default for local dev/testing
        // Placeholder for real GeoIP logic (MaxMind/IP-API)
        return "IR" // Most of users will be from Iran given the context
    }
}
