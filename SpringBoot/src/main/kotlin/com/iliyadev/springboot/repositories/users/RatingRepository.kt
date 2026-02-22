package com.iliyadev.springboot.repositories.users

import com.iliyadev.springboot.models.users.Rating
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RatingRepository : JpaRepository<Rating, Long> {
    fun findByToUserIdOrderByCreatedAtDesc(toUserId: Long, pageable: Pageable): Page<Rating>
    
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.toUser.id = :userId")
    fun getAverageRating(@Param("userId") userId: Long): Double?
    
    fun countByToUserId(toUserId: Long): Long
}
