package com.iliyadev.springboot.repositories.users

import com.iliyadev.springboot.models.users.FollowedCompany
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FollowedCompanyRepository : JpaRepository<FollowedCompany, Long> {
    fun findByUserIdOrderByFollowedAtDesc(userId: Long, pageable: Pageable): Page<FollowedCompany>
    fun existsByUserIdAndEmployerId(userId: Long, employerId: Long): Boolean
    fun deleteByUserIdAndEmployerId(userId: Long, employerId: Long)
}
