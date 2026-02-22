package com.iliyadev.springboot.models.users

import com.iliyadev.springboot.models.customers.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "resumes")
data class Resume(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val fileName: String = "",           // نام فایل
    val originalFileName: String = "",   // نام اصلی فایل
    val filePath: String = "",           // مسیر فایل
    val fileSize: Long = 0,              // اندازه فایل
    val mimeType: String = "",           // نوع فایل
    
    val uploadedAt: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null
)
