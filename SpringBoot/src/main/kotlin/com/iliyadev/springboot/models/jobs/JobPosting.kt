package com.iliyadev.springboot.models.jobs

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.models.enums.*
import com.iliyadev.springboot.models.locations.City
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "job_postings")
data class JobPosting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val title: String = "",              // عنوان شغلی
    
    @Column(columnDefinition = "TEXT")
    val description: String = "",        // توضیحات شغل
    
    @Column(columnDefinition = "TEXT")
    val jobDescription: String? = null,  // شرح شغل و وظایف
    
    // ========== حقوق ==========
    val salaryMin: Long? = null,         // حداقل حقوق
    val salaryMax: Long? = null,         // حداکثر حقوق
    
    @Enumerated(EnumType.STRING)
    val salaryType: SalaryType = SalaryType.NEGOTIABLE,  // نوع حقوق
    
    // ========== شرایط کاری ==========
    val workSchedule: String? = null,    // روز و ساعت کاری (مثلاً شنبه تا چهارشنبه 7:45 تا 17:00)
    val businessTrips: String? = null,   // سفرهای کاری
    val isRemote: Boolean = false,       // امکان دورکاری
    val isInternship: Boolean = false,   // کارآموزی
    
    // ========== نیازمندی‌ها ==========
    @Column(columnDefinition = "TEXT")
    val requirements: String? = null,    // نیازمندی‌های عمومی
    
    @Column(columnDefinition = "TEXT")
    val keyRequirements: String? = null, // شاخص‌های کلیدی کارفرما (JSON array)
    
    val experienceYears: Int? = null,    // سال‌های سابقه کار
    val experienceDescription: String? = null, // توضیح سابقه کار
    
    @Enumerated(EnumType.STRING)
    val seniorityLevel: SeniorityLevel? = null, // سطح ارشدیت
    
    @Enumerated(EnumType.STRING)
    val educationLevel: EducationLevel? = null, // سطح تحصیلات
    
    val educationField: String? = null,  // رشته تحصیلی
    
    @Enumerated(EnumType.STRING)
    val gender: Gender = Gender.ANY,     // جنسیت مورد نیاز
    
    @Column(columnDefinition = "TEXT")
    val languages: String? = null,       // زبان‌ها (JSON array: [{name, level}])
    
    @Column(columnDefinition = "TEXT")
    val softwareSkills: String? = null,  // نرم‌افزارها (JSON array: [{name, level}])
    
    // ========== مزایا ==========
    @Column(columnDefinition = "TEXT")
    val benefits: String? = null,        // مزایا و تسهیلات (JSON array)
    
    // ========== صنعت ==========
    @Enumerated(EnumType.STRING)
    val industry: Industry? = null,      // صنعت
    
    // ========== شرایط خاص ==========
    val disabilityFriendly: Boolean = false,      // امکان استخدام معلولین
    val militaryServiceExemption: Boolean = false, // امریه سربازی
    
    // ========== اطلاعات شرکت ==========
    val companyName: String? = null,     // نام شرکت
    val companyLogo: String? = null,     // لوگوی شرکت
    
    @Column(columnDefinition = "TEXT")
    val companyDescription: String? = null, // درباره شرکت
    
    val companySize: String? = null,     // اندازه شرکت
    val companyWebsite: String? = null,  // وب‌سایت شرکت
    
    // ========== وضعیت ==========
    val isActive: Boolean = true,
    val isGovernment: Boolean = false,   // استخدام دولتی
    val isUrgent: Boolean = false,       // فوری
    val isResponsive: Boolean = false,   // کارفرمای پاسخگو
    
    // ========== آمار ==========
    val viewCount: Int = 0,              // تعداد بازدید
    val applicationCount: Int = 0,       // تعداد درخواست
    
    // ========== تاریخ ==========
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime? = null,
    
    // ========== روابط ==========
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employer_id")
    val employer: User? = null,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    val category: JobCategory? = null,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id")
    val city: City? = null,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cooperation_type_id")
    val cooperationType: CooperationType? = null
)
