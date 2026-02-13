package com.iliyadev.minichat.core.security

import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found") }
            
        return org.springframework.security.core.userdetails.User
            .withUsername(user.username)
            .authorities(user.role.name) // PRODUCTION: Using real DB role
            .password(user.password ?: "") // BCrypt hash from DB
            .accountExpired(false)
            .accountLocked(user.isBanned)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }
}
