package com.iliyadev.springboot.models.jobs

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "job_categories")
data class JobCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val name: String = "",       // نام دسته‌بندی
    val icon: String? = null,    // آیکون دسته‌بندی
    
    @JsonIgnore
    @OneToMany(mappedBy = "category")
    val jobs: Set<JobPosting>? = null
)
