package com.iliyadev.springboot.models.customers

import com.fasterxml.jackson.annotation.JsonIgnore
import com.iliyadev.springboot.models.enums.UserType
import com.iliyadev.springboot.models.jobs.JobApplication
import com.iliyadev.springboot.models.jobs.JobPosting
import com.iliyadev.springboot.models.users.*
import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    val username: String = "",
    var password: String = "",
    val email: String? = null,           // ایمیل
    val phone: String? = null,           // شماره تلفن
    
    val isAdmin: Boolean = false,
    
    @Enumerated(EnumType.STRING)
    val userType: UserType = UserType.JOBSEEKER,  // نوع کاربر
    
    // اطلاعات پروفایل
    val fullName: String? = null,        // نام کامل
    val profileImage: String? = null,    // عکس پروفایل
    val bio: String? = null,             // درباره من
    
    // اطلاعات کارفرما
    val companyName: String? = null,     // نام شرکت
    val companyLogo: String? = null,     // لوگوی شرکت
    val companyDescription: String? = null,  // توضیحات شرکت
    val companyWebsite: String? = null,  // وب‌سایت شرکت
    val companySize: String? = null,     // اندازه شرکت
    
    // اطلاعات کارجو
    val skills: String? = null,          // مهارت‌ها
    val experience: String? = null,      // سابقه کار
    val education: String? = null,       // تحصیلات
    
    val createdAt: String? = null,       // تاریخ ثبت‌نام
    
    // روابط
    @JsonIgnore
    @OneToMany(mappedBy = "employer")
    val postedJobs: Set<JobPosting>? = null,  // آگهی‌های ثبت شده
    
    @JsonIgnore
    @OneToMany(mappedBy = "applicant")
    val applications: Set<JobApplication>? = null,  // درخواست‌های کار
    
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    val resumes: Set<Resume>? = null,    // رزومه‌ها
    
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    val bookmarks: Set<Bookmark>? = null,  // نشان‌شده‌ها
    
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    val notifications: Set<Notification>? = null,  // اعلان‌ها
    
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    val followedCompanies: Set<FollowedCompany>? = null,  // شرکت‌های دنبال شده
    
    @JsonIgnore
    @OneToMany(mappedBy = "toUser")
    val receivedRatings: Set<Rating>? = null  // امتیازهای دریافتی
)