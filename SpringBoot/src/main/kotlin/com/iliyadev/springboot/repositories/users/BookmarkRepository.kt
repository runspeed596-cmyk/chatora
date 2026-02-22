package com.iliyadev.springboot.repositories.users

import com.iliyadev.springboot.models.users.Bookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookmarkRepository : JpaRepository<Bookmark, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Bookmark>
    fun existsByUserIdAndJobId(userId: Long, jobId: Long): Boolean
    fun deleteByUserIdAndJobId(userId: Long, jobId: Long)
}
