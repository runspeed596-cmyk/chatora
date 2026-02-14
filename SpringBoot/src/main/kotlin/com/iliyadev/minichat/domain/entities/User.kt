package com.iliyadev.minichat.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    var username: String = "",

    @Column(nullable = false)
    var deviceId: String = "", // For fingerprinting/banning

    @Column(unique = true)
    var email: String? = null,

    var password: String? = null,

    @Column(unique = true)
    var googleId: String? = null,

    @Enumerated(EnumType.STRING)
    var gender: Gender = Gender.UNSPECIFIED,

    var countryCode: String? = null,
    
    var languageCode: String? = "en",

    var karma: Int = 100,

    var diamonds: Int = 0,

    var isBanned: Boolean = false,
    
    var banReason: String? = null,

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    var emailVerified: Boolean = false,

    @Column(name = "verification_code")
    var verificationCode: String? = null,

    var isPremium: Boolean = false,

    @Column(name = "premium_until")
    var premiumUntil: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    var role: Role = Role.USER,

    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class Gender {
    MALE, FEMALE, UNSPECIFIED
}

enum class Role {
    USER, ADMIN
}
