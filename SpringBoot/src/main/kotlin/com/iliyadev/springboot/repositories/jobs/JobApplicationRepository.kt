package com.iliyadev.springboot.repositories.jobs

import com.iliyadev.springboot.models.jobs.JobApplication
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobApplicationRepository : JpaRepository<JobApplication, Long> {
    
    // درخواست‌های یک کارجو
    fun findByApplicantIdOrderByAppliedAtDesc(applicantId: Long, pageable: Pageable): Page<JobApplication>
    
    // درخواست‌های یک آگهی
    fun findByJobIdOrderByAppliedAtDesc(jobId: Long, pageable: Pageable): Page<JobApplication>
    
    // درخواست‌های کارفرما (از طریق آگهی‌ها)
    fun findByJobEmployerIdOrderByAppliedAtDesc(employerId: Long, pageable: Pageable): Page<JobApplication>
    
    // بررسی تکراری نبودن درخواست
    fun existsByApplicantIdAndJobId(applicantId: Long, jobId: Long): Boolean
    
    // تعداد درخواست‌های یک آگهی
    fun countByJobId(jobId: Long): Long
}

