package com.iliyadev.springboot.repositories.users

import com.iliyadev.springboot.models.users.Resume
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ResumeRepository : JpaRepository<Resume, Long> {
    fun findByUserId(userId: Long): List<Resume>
    fun findFirstByUserIdOrderByUploadedAtDesc(userId: Long): Resume?
}
