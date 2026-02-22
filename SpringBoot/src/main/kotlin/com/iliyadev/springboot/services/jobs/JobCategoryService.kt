package com.iliyadev.springboot.services.jobs

import com.iliyadev.springboot.models.jobs.JobCategory
import com.iliyadev.springboot.repositories.jobs.JobCategoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JobCategoryService {
    
    @Autowired
    private lateinit var repository: JobCategoryRepository
    
    fun getAll(): List<JobCategory> {
        val categories = repository.findAll()
        // فیلتر کردن دسته‌بندی‌هایی که آگهی فعال دارند
        // نکته: این روش برای تعداد زیاد داده بهینه نیست، اما فعلاً کار راه انداز است
        return categories.filter { category ->
            category.jobs?.any { it.isActive } == true
        }
    }
    
    fun getById(id: Long): JobCategory? = repository.findById(id).orElse(null)
    
    fun search(name: String): List<JobCategory> = repository.findByNameContainingIgnoreCase(name)
    
    fun create(category: JobCategory): JobCategory = repository.save(category)
    
    fun update(id: Long, category: JobCategory): JobCategory? {
        return if (repository.existsById(id)) {
            repository.save(category.copy(id = id))
        } else null
    }
    
    fun delete(id: Long): Boolean {
        return if (repository.existsById(id)) {
            repository.deleteById(id)
            true
        } else false
    }
}
