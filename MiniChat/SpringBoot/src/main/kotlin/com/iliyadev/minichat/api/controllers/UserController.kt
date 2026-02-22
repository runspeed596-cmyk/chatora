package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.api.dtos.UserDto
import com.iliyadev.minichat.core.response.ApiResponse
import com.iliyadev.minichat.domain.repositories.UserRepository
import com.iliyadev.minichat.domain.services.SubscriptionService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(
    private val userRepository: UserRepository,
    private val subscriptionService: SubscriptionService,
    private val matchController: MatchController
) {

    @GetMapping("/me")
    fun getMe(): ApiResponse<UserDto> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        val dto = UserDto(
            id = user.id.toString(),
            username = user.username,
            email = user.email,
            googleId = user.googleId,
            karma = user.karma,
            diamonds = user.diamonds,
            countryCode = user.countryCode ?: "US",
            languageCode = user.languageCode ?: "en",
            gender = user.gender.name,
            isPremium = subscriptionService.isPremiumUser(user.id!!),
            isBanned = user.isBanned
        )

        return ApiResponse.success(dto)
    }

    @PostMapping("/gender")
    fun updateGender(@RequestBody request: Map<String, String>): ApiResponse<String> {
        val username = SecurityContextHolder.getContext().authentication.name
        val genderStr = request["gender"] ?: throw IllegalArgumentException("Gender is required")
        
        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        user.gender = com.iliyadev.minichat.domain.entities.Gender.valueOf(genderStr.uppercase())
        userRepository.save(user)

        // CLEAR MATCH CACHE to ensure gender is updated in matching engine instantly
        matchController.clearCacheForUser(username)

        return ApiResponse.success("Gender updated successfully")
    }
}
