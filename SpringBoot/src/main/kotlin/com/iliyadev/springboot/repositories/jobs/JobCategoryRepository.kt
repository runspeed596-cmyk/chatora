package com.iliyadev.springboot.repositories.jobs

import com.iliyadev.springboot.models.jobs.JobCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobCategoryRepository : JpaRepository<JobCategory, Long> {
    fun findByNameContainingIgnoreCase(name: String): List<JobCategory>
}
