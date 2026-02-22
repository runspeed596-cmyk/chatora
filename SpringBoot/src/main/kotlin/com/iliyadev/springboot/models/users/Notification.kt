package com.iliyadev.springboot.models.users

import com.iliyadev.springboot.models.customers.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val title: String = "",              // عنوان اعلان
    val message: String = "",            // متن اعلان
    val type: String = "",               // نوع اعلان (job, application, system)
    val isRead: Boolean = false,         // خوانده شده
    
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val relatedJobId: Long? = null,      // شناسه آگهی مرتبط
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null
)
