package com.iliyadev.springboot.services.jobs

import com.iliyadev.springboot.models.jobs.JobPosting
import com.iliyadev.springboot.repositories.jobs.JobPostingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class JobPostingService {
    
    @Autowired
    private lateinit var repository: JobPostingRepository
    
    // دریافت آگهی‌ها با صفحه‌بندی
    fun getAll(pageIndex: Int, pageSize: Int): Page<JobPosting> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize)
        return repository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
    }
    
    // دریافت یک آگهی
    fun getById(id: Long): JobPosting? = repository.findById(id).orElse(null)
    
    // افزایش تعداد بازدید
    fun incrementViewCount(id: Long): JobPosting? {
        val job = repository.findById(id).orElse(null) ?: return null
        return repository.save(job.copy(viewCount = job.viewCount + 1))
    }
    
    // جستجوی پیشرفته
    fun advancedSearch(
        title: String?,
        categoryId: Long?,
        cityId: Long?,
        cooperationTypeId: Long?,
        isRemote: Boolean?,
        isInternship: Boolean?,
        isGovernment: Boolean?,
        salaryMin: Long?,
        salaryMax: Long?,
        disabilityFriendly: Boolean?,
        militaryServiceExemption: Boolean?,
        pageIndex: Int,
        pageSize: Int
    ): Page<JobPosting> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize)
        return repository.advancedSearch(
            title = title,
            categoryId = categoryId,
            cityId = cityId,
            cooperationTypeId = cooperationTypeId,
            isRemote = isRemote,
            isInternship = isInternship,
            isGovernment = isGovernment,
            salaryMin = salaryMin,
            salaryMax = salaryMax,
            seniorityLevel = null,
            educationLevel = null,
            industry = null,
            disabilityFriendly = disabilityFriendly,
            militaryServiceExemption = militaryServiceExemption,
            createdAfter = null,
            pageable = pageable
        )
    }
    
    // جستجوی ساده (برای backward compatibility)
    fun search(
        title: String?,
        categoryId: Long?,
        cityId: Long?,
        cooperationTypeId: Long?,
        pageIndex: Int,
        pageSize: Int
    ): Page<JobPosting> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize)
        return repository.searchJobs(title, categoryId, cityId, cooperationTypeId, pageable)
    }
    
    // آگهی‌های دولتی
    fun getGovernmentJobs(pageIndex: Int, pageSize: Int): Page<JobPosting> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize)
        return repository.findByIsGovernmentTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable)
    }
    
    // آگهی‌های یک دسته‌بندی
    fun getByCategory(categoryId: Long, pageIndex: Int, pageSize: Int): Page<JobPosting> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize)
        return repository.findByCategoryIdAndIsActiveTrue(categoryId, pageable)
    }
    
    // آگهی‌های یک شهر
    fun getByCity(cityId: Long, pageIndex: Int, pageSize: Int): Page<JobPosting> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize)
        return repository.findByCityIdAndIsActiveTrue(cityId, pageable)
    }
    
    // آگهی‌های یک کارفرما (همه آگهی‌ها برای داشبورد)
    fun getByEmployer(employerId: Long, pageIndex: Int, pageSize: Int): Page<JobPosting> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize)
        return repository.findByEmployerIdOrderByCreatedAtDesc(employerId, pageable)
    }
    
    // تعداد کل آگهی‌ها
    fun getTotalCount(): Long = repository.countByIsActiveTrue()
    
    // تعداد شهرها
    fun getCitiesCount(): Long = repository.countDistinctCities()
    
    // جدیدترین آگهی‌ها
    fun getLatest(): List<JobPosting> = repository.findTop10ByIsActiveTrueOrderByCreatedAtDesc()
    
    // ایجاد آگهی
    fun create(job: JobPosting): JobPosting = repository.save(job)
    
    // به‌روزرسانی آگهی
    fun update(id: Long, job: JobPosting): JobPosting? {
        return if (repository.existsById(id)) {
            repository.save(job.copy(id = id))
        } else null
    }
    
    // حذف آگهی
    fun delete(id: Long): Boolean {
        return if (repository.existsById(id)) {
            repository.deleteById(id)
            true
        } else false
    }
    
    // غیرفعال کردن آگهی
    fun deactivate(id: Long): JobPosting? {
        val job = repository.findById(id).orElse(null) ?: return null
        return repository.save(job.copy(isActive = false))
    }
}
