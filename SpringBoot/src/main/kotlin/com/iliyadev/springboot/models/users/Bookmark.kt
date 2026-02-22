package com.iliyadev.springboot.models.users

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.models.jobs.JobPosting
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "bookmarks")
data class Bookmark(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    val user: User? = null,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id")
    val job: JobPosting? = null
)
