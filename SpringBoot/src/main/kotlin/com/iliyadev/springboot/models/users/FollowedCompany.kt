package com.iliyadev.springboot.models.users

import com.iliyadev.springboot.models.customers.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "followed_companies")
data class FollowedCompany(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val companyName: String = "",        // نام شرکت
    val followedAt: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    val user: User? = null,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employer_id")
    val employer: User? = null           // کارفرما/شرکت
)
