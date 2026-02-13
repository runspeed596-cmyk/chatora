package com.iliyadev.minichat.domain.usecases.auth

import com.iliyadev.minichat.api.dtos.*
import com.iliyadev.minichat.core.exception.*
import com.iliyadev.minichat.core.security.JwtService
import com.iliyadev.minichat.domain.entities.User
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EmailAuthUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: com.iliyadev.minichat.domain.services.EmailService
) {
    @Transactional
    fun register(request: EmailRegisterRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        if (userRepository.existsByEmail(email)) {
            throw ConflictException("EMAIL_ALREADY_EXISTS", "This email is already registered.")
        }
        if (userRepository.existsByUsername(request.username)) {
            throw ConflictException("USERNAME_ALREADY_EXISTS", "This username is already taken.")
        }

        val code = String.format("%06d", Random().nextInt(999999))
        
        val newUser = User(
            username = request.username.trim(),
            email = email,
            password = passwordEncoder.encode(request.password),
            deviceId = request.deviceId,
            countryCode = request.countryCode,
            languageCode = request.languageCode,
            emailVerified = false,
            verificationCode = code
        )
        val savedUser = userRepository.save(newUser)

        // Send real email
        try {
            emailService.sendVerificationCode(email, code)
        } catch (e: Exception) {
            println("Warning: Failed to send email to $email, but user was created: ${e.message}")
        }

        return generateAuthResponse(savedUser, verificationCode = code)
    }

    @Transactional
    fun resendCode(email: String): String {
        val user = userRepository.findByEmail(email.trim().lowercase())
            .orElseThrow { UserNotFoundException("No account found with this email address.") }

        if (user.emailVerified) {
            throw ApiException(code = "ALREADY_VERIFIED", message = "This email is already verified.")
        }

        val code = String.format("%06d", Random().nextInt(999999))
        user.verificationCode = code
        userRepository.save(user)

        emailService.sendVerificationCode(user.email!!, code)
        return code
    }

    @Transactional
    fun login(request: EmailLoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
            .orElseThrow { InvalidCredentialsException("No account found with this email. Please register first.") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException("Incorrect password. Please try again.")
        }

        if (user.isBanned) {
            throw BannedException(user.banReason)
        }

        if (!user.emailVerified) {
            throw EmailNotVerifiedException()
        }
        
        // Update device ID
        if (user.deviceId != request.deviceId) {
            user.deviceId = request.deviceId
            userRepository.save(user)
        }

        return generateAuthResponse(user)
    }

    @Transactional
    fun verifyEmail(request: VerifyEmailRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException("User not found.") }

        if (user.verificationCode != request.code) {
            throw ApiException(code = "INVALID_CODE", message = "The verification code you entered is invalid.")
        }

        user.emailVerified = true
        user.verificationCode = null
        val savedUser = userRepository.save(user)

        return generateAuthResponse(savedUser)
    }

    private fun generateAuthResponse(user: User, verificationCode: String? = null): AuthResponse {
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
            tempUsername = false,
            emailVerified = user.emailVerified,
            verificationCode = verificationCode
        )
    }
}
