package com.iliyadev.springboot.models.jobs

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.models.enums.ApplicationStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "job_applications")
data class JobApplication(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    @Enumerated(EnumType.STRING)
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    
    val coverLetter: String? = null,     // نامه همراه
    val appliedAt: LocalDateTime = LocalDateTime.now(),
    val reviewedAt: LocalDateTime? = null,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id")
    val job: JobPosting? = null,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applicant_id")
    val applicant: User? = null
)
