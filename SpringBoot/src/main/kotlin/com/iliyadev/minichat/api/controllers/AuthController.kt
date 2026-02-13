package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.api.dtos.*
import com.iliyadev.minichat.core.security.GoogleAuthService
import com.iliyadev.minichat.core.response.ApiResponse
import com.iliyadev.minichat.domain.usecases.auth.*
import jakarta.validation.Valid
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
    private val emailAuthUseCase: EmailAuthUseCase
) {

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
    fun googleLogin(@Valid @RequestBody request: GoogleLoginRequest): ApiResponse<AuthResponse> {
        val response = googleLoginUseCase.execute(request)
        return ApiResponse.success(response, "Google login successful")
    }

    @PostMapping("/email/login")
    fun emailLogin(@Valid @RequestBody request: EmailLoginRequest): ApiResponse<AuthResponse> {
        val response = emailAuthUseCase.login(request)
        return ApiResponse.success(response, "Email login successful")
    }

    @PostMapping("/email/register")
    fun emailRegister(@Valid @RequestBody request: EmailRegisterRequest): ApiResponse<AuthResponse> {
        val response = emailAuthUseCase.register(request)
        return ApiResponse.success(response, "Registration successful. Please verify your email.")
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ApiResponse<AuthResponse> {
        val response = emailAuthUseCase.verifyEmail(request)
        return ApiResponse.success(response, "Email verified successfully")
    }

    @PostMapping("/email/resend-code")
    fun resendCode(@Valid @RequestBody request: EmailResendRequest): ApiResponse<String> {
        emailAuthUseCase.resendCode(request.email)
        return ApiResponse.success("CODE_SENT", "Verification code resent successfully")
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ApiResponse<String?> {
        // TODO: Implement refresh logic
        return ApiResponse.error("NOT_IMPLEMENTED", "Refresh not implemented yet") as ApiResponse<String?>
    }
}
