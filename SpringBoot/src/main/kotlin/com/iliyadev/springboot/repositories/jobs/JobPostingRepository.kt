package com.iliyadev.springboot.repositories.jobs

import com.iliyadev.springboot.models.enums.*
import com.iliyadev.springboot.models.jobs.JobPosting
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface JobPostingRepository : JpaRepository<JobPosting, Long> {
    
    // صفحه‌بندی آگهی‌های فعال
    fun findByIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): Page<JobPosting>
    
    // جستجو بر اساس عنوان
    fun findByTitleContainingIgnoreCaseAndIsActiveTrue(title: String, pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های یک دسته‌بندی
    fun findByCategoryIdAndIsActiveTrue(categoryId: Long, pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های یک شهر
    fun findByCityIdAndIsActiveTrue(cityId: Long, pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های دولتی
    fun findByIsGovernmentTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های یک کارفرما (فقط فعال‌ها - برای نمایش عمومی)
    fun findByEmployerIdAndIsActiveTrue(employerId: Long, pageable: Pageable): Page<JobPosting>
    
    // همه آگهی‌های یک کارفرما (برای داشبورد کارفرما)
    fun findByEmployerIdOrderByCreatedAtDesc(employerId: Long, pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های دورکاری
    fun findByIsRemoteTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های کارآموزی
    fun findByIsInternshipTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های امکان استخدام معلولین
    fun findByDisabilityFriendlyTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): Page<JobPosting>
    
    // آگهی‌های امریه سربازی
    fun findByMilitaryServiceExemptionTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable: Pageable): Page<JobPosting>
    
    // تعداد کل آگهی‌های فعال
    fun countByIsActiveTrue(): Long
    
    // تعداد شهرهای دارای آگهی
    @Query("SELECT COUNT(DISTINCT j.city.id) FROM JobPosting j WHERE j.isActive = true")
    fun countDistinctCities(): Long
    
    // جستجوی پیشرفته با همه فیلترها
    @Query("""
        SELECT j FROM JobPosting j 
        WHERE j.isActive = true 
        AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:categoryId IS NULL OR j.category.id = :categoryId)
        AND (:cityId IS NULL OR j.city.id = :cityId)
        AND (:cooperationTypeId IS NULL OR j.cooperationType.id = :cooperationTypeId)
        AND (:isRemote IS NULL OR j.isRemote = :isRemote)
        AND (:isInternship IS NULL OR j.isInternship = :isInternship)
        AND (:isGovernment IS NULL OR j.isGovernment = :isGovernment)
        AND (:salaryMin IS NULL OR j.salaryMin >= :salaryMin)
        AND (:salaryMax IS NULL OR j.salaryMax <= :salaryMax)
        AND (:seniorityLevel IS NULL OR j.seniorityLevel = :seniorityLevel)
        AND (:educationLevel IS NULL OR j.educationLevel = :educationLevel)
        AND (:industry IS NULL OR j.industry = :industry)
        AND (:disabilityFriendly IS NULL OR j.disabilityFriendly = :disabilityFriendly)
        AND (:militaryServiceExemption IS NULL OR j.militaryServiceExemption = :militaryServiceExemption)
        AND (:createdAfter IS NULL OR j.createdAt >= :createdAfter)
        ORDER BY j.createdAt DESC
    """)
    fun advancedSearch(
        @Param("title") title: String?,
        @Param("categoryId") categoryId: Long?,
        @Param("cityId") cityId: Long?,
        @Param("cooperationTypeId") cooperationTypeId: Long?,
        @Param("isRemote") isRemote: Boolean?,
        @Param("isInternship") isInternship: Boolean?,
        @Param("isGovernment") isGovernment: Boolean?,
        @Param("salaryMin") salaryMin: Long?,
        @Param("salaryMax") salaryMax: Long?,
        @Param("seniorityLevel") seniorityLevel: SeniorityLevel?,
        @Param("educationLevel") educationLevel: EducationLevel?,
        @Param("industry") industry: Industry?,
        @Param("disabilityFriendly") disabilityFriendly: Boolean?,
        @Param("militaryServiceExemption") militaryServiceExemption: Boolean?,
        @Param("createdAfter") createdAfter: LocalDateTime?,
        pageable: Pageable
    ): Page<JobPosting>
    
    // جستجوی ساده (برای سازگاری با قبل)
    @Query("""
        SELECT j FROM JobPosting j 
        WHERE j.isActive = true 
        AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:categoryId IS NULL OR j.category.id = :categoryId)
        AND (:cityId IS NULL OR j.city.id = :cityId)
        AND (:cooperationTypeId IS NULL OR j.cooperationType.id = :cooperationTypeId)
        ORDER BY j.createdAt DESC
    """)
    fun searchJobs(
        @Param("title") title: String?,
        @Param("categoryId") categoryId: Long?,
        @Param("cityId") cityId: Long?,
        @Param("cooperationTypeId") cooperationTypeId: Long?,
        pageable: Pageable
    ): Page<JobPosting>
    
    // جدیدترین آگهی‌ها
    fun findTop10ByIsActiveTrueOrderByCreatedAtDesc(): List<JobPosting>
    
    // آگهی‌های مشابه (همان دسته‌بندی، غیر از خودش)
    @Query("""
        SELECT j FROM JobPosting j 
        WHERE j.isActive = true 
        AND j.category.id = :categoryId 
        AND j.id != :excludeJobId
        ORDER BY j.createdAt DESC
    """)
    fun findSimilarJobs(
        @Param("categoryId") categoryId: Long,
        @Param("excludeJobId") excludeJobId: Long,
        pageable: Pageable
    ): Page<JobPosting>
    
    // سایر آگهی‌های یک شرکت
    @Query("""
        SELECT j FROM JobPosting j 
        WHERE j.isActive = true 
        AND j.employer.id = :employerId 
        AND j.id != :excludeJobId
        ORDER BY j.createdAt DESC
    """)
    fun findOtherJobsByCompany(
        @Param("employerId") employerId: Long,
        @Param("excludeJobId") excludeJobId: Long,
        pageable: Pageable
    ): Page<JobPosting>
}
