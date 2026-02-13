package com.iliyadev.minichat.data.repositories

import com.iliyadev.minichat.domain.entities.User
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

// Internal JpaRepository, hidden from Domain
@Repository
interface JpaUserRepo : JpaRepository<User, UUID> {
    fun findByUsername(username: String): Optional<User>
    fun findByDeviceId(deviceId: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun findByGoogleId(googleId: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun countByUpdatedAtAfter(date: java.time.LocalDateTime): Long

    @org.springframework.data.jpa.repository.Query("SELECT u.countryCode, COUNT(u) as count FROM User u GROUP BY u.countryCode")
    fun countByCountryCode(): List<Array<Any>>
}

@Component
class UserRepositoryImpl(
    private val jpaRepo: JpaUserRepo
) : UserRepository {
    
    override fun save(user: User): User = jpaRepo.save(user)
    
    override fun findById(id: UUID): Optional<User> = jpaRepo.findById(id)
    
    override fun findByUsername(username: String): Optional<User> = jpaRepo.findByUsername(username)
    
    override fun findByDeviceId(deviceId: String): Optional<User> = jpaRepo.findByDeviceId(deviceId)
    
    override fun findByEmail(email: String): Optional<User> = jpaRepo.findByEmail(email)
    
    override fun findByGoogleId(googleId: String): Optional<User> = jpaRepo.findByGoogleId(googleId)
    
    override fun existsByUsername(username: String): Boolean = jpaRepo.existsByUsername(username)
    
    override fun existsByEmail(email: String): Boolean = jpaRepo.existsByEmail(email)

    override fun findAll(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<User> = jpaRepo.findAll(pageable)

    override fun count(): Long = jpaRepo.count()

    override fun deleteById(id: UUID) = jpaRepo.deleteById(id)

    override fun countByUpdatedAtAfter(date: java.time.LocalDateTime): Long = jpaRepo.countByUpdatedAtAfter(date)

    override fun countByCountryCode(): List<Array<Any>> = jpaRepo.countByCountryCode()
}
