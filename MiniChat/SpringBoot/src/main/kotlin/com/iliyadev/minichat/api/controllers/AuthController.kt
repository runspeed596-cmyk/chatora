package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.api.dtos.*
import com.iliyadev.minichat.core.security.GoogleAuthService
import com.iliyadev.minichat.core.security.JwtService
import com.iliyadev.minichat.core.security.CustomUserDetailsService
import com.iliyadev.minichat.core.response.ApiResponse
import com.iliyadev.minichat.domain.usecases.auth.*
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val emailAuthUseCase: EmailAuthUseCase,
    private val jwtService: JwtService,
    private val userDetailsService: CustomUserDetailsService
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @GetMapping("/health")
    fun health(): ApiResponse<Map<String, String>> {
        return ApiResponse.success(
            mapOf("status" to "UP", "service" to "Chatora API"),
            "Service is healthy"
        )
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<AuthResponse> {
        val remoteIp = servletRequest.remoteAddr
        val response = loginUseCase.execute(request, remoteIp)
        return ApiResponse.success(response, "Login successful")
    }

    @PostMapping("/google")
    fun googleLogin(
        @Valid @RequestBody request: GoogleLoginRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<AuthResponse> {
        val remoteIp = servletRequest.remoteAddr
        val response = googleLoginUseCase.execute(request, remoteIp)
        return ApiResponse.success(response, "Google login successful")
    }

    @PostMapping("/email/login")
    fun emailLogin(
        @Valid @RequestBody request: EmailLoginRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<AuthResponse> {
        val remoteIp = servletRequest.remoteAddr
        val response = emailAuthUseCase.login(request, remoteIp)
        return ApiResponse.success(response, "Email login successful")
    }

    @PostMapping("/email/register")
    fun emailRegister(
        @Valid @RequestBody request: EmailRegisterRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<AuthResponse> {
        val remoteIp = servletRequest.remoteAddr
        val response = emailAuthUseCase.register(request, remoteIp)
        return ApiResponse.success(response, "Registration successful. Please verify your email.")
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ApiResponse<AuthResponse> {
        val response = emailAuthUseCase.verifyEmail(request)
        return ApiResponse.success(response, "Email verified successfully")
    }

    @PostMapping("/email/resend-code")
    fun resendCode(@Valid @RequestBody request: EmailResendRequest): ApiResponse<Map<String, String>> {
        val code = emailAuthUseCase.resendCode(request.email)
        return ApiResponse.success(mapOf("verificationCode" to code), "Verification code resent successfully")
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ApiResponse<AuthResponse> {
        return try {
            val username = jwtService.extractUsername(request.refreshToken)
            val userDetails = userDetailsService.loadUserByUsername(username)

            if (!jwtService.isTokenValid(request.refreshToken, userDetails)) {
                logger.warn("Refresh token invalid for user: {}", username)
                @Suppress("UNCHECKED_CAST")
                return ApiResponse.error("INVALID_TOKEN", "Refresh token is invalid") as ApiResponse<AuthResponse>
            }

            val newAccessToken = jwtService.generateToken(userDetails)
            val newRefreshToken = jwtService.generateRefreshToken(userDetails)

            logger.info("Token refreshed successfully for user: {}", username)

            ApiResponse.success(
                AuthResponse(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    userId = username, // Will be resolved by the client from the token
                    username = username,
                    tempUsername = false,
                    emailVerified = true
                ),
                "Token refreshed successfully"
            )
        } catch (e: ExpiredJwtException) {
            logger.warn("Refresh token expired: {}", e.message)
            @Suppress("UNCHECKED_CAST")
            ApiResponse.error("TOKEN_EXPIRED", "Refresh token has expired. Please login again.") as ApiResponse<AuthResponse>
        } catch (e: JwtException) {
            logger.warn("Invalid refresh token: {}", e.message)
            @Suppress("UNCHECKED_CAST")
            ApiResponse.error("INVALID_TOKEN", "Invalid refresh token") as ApiResponse<AuthResponse>
        } catch (e: Exception) {
            logger.error("Refresh token error", e)
            @Suppress("UNCHECKED_CAST")
            ApiResponse.error("REFRESH_FAILED", "Failed to refresh token") as ApiResponse<AuthResponse>
        }
    }
}
