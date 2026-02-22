package com.iliyadev.minichat.domain.repositories

import com.iliyadev.minichat.domain.entities.User
import java.util.UUID
import java.util.Optional

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UUID): Optional<User>
    fun findByUsername(username: String): Optional<User>
    fun findByDeviceId(deviceId: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun findByGoogleId(googleId: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    
    // Admin features
    fun findAll(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<User>
    fun count(): Long
    fun deleteById(id: UUID)
    fun countByUpdatedAtAfter(date: java.time.LocalDateTime): Long
    fun countByCountryCode(): List<Array<Any>>

    // Admin search & filter
    fun searchByUsernameOrEmail(search: String, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<User>
    fun findByIsBanned(isBanned: Boolean, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<User>
    fun searchByUsernameOrEmailAndIsBanned(search: String, isBanned: Boolean, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<User>
}
