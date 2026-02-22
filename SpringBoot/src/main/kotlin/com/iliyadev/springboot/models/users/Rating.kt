package com.iliyadev.springboot.models.users

import com.iliyadev.springboot.models.customers.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ratings")
data class Rating(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val score: Int = 0,                  // امتیاز (1-5)
    val comment: String? = null,         // نظر
    
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_user_id")
    val fromUser: User? = null,          // کاربری که امتیاز داده
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_user_id")
    val toUser: User? = null             // کاربری که امتیاز گرفته
)
