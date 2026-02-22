package com.iliyadev.minichat.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "reports")
data class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val reporterId: String = "",

    @Column(nullable = false)
    val reportedUserId: String = "",

    @Column(nullable = false)
    val reason: String = "",

    val description: String? = null,

    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)
