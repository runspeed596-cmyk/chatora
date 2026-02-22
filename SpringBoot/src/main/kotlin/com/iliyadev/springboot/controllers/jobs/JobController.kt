package com.iliyadev.springboot.controllers.jobs

import com.iliyadev.springboot.models.jobs.JobPosting
import com.iliyadev.springboot.services.jobs.JobPostingService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/jobs")
@CrossOrigin
class JobController {
    
    @Autowired
    private lateinit var service: JobPostingService
    
    // لیست آگهی‌ها با صفحه‌بندی
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int
    ): ServiceResponse<Page<JobPosting>> {
        return try {
            val data = service.getAll(pageIndex, pageSize)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جزئیات آگهی
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ServiceResponse<JobPosting> {
        return try {
            val data = service.getById(id)
            if (data != null) {
                service.incrementViewCount(id) // افزایش تعداد بازدید
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "آگهی یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جستجوی پیشرفته
    @GetMapping("/search")
    fun search(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) cityId: Long?,
        @RequestParam(required = false) cooperationTypeId: Long?,
        @RequestParam(required = false) isRemote: Boolean?,
        @RequestParam(required = false) isInternship: Boolean?,
        @RequestParam(required = false) isGovernment: Boolean?,
        @RequestParam(required = false) salaryMin: Long?,
        @RequestParam(required = false) salaryMax: Long?,
        @RequestParam(required = false) disabilityFriendly: Boolean?,
        @RequestParam(required = false) militaryServiceExemption: Boolean?,
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int
    ): ServiceResponse<Page<JobPosting>> {
        return try {
            val data = service.advancedSearch(
                title = title,
                categoryId = categoryId,
                cityId = cityId,
                cooperationTypeId = cooperationTypeId,
                isRemote = isRemote,
                isInternship = isInternship,
                isGovernment = isGovernment,
                salaryMin = salaryMin,
                salaryMax = salaryMax,
                disabilityFriendly = disabilityFriendly,
                militaryServiceExemption = militaryServiceExemption,
                pageIndex = pageIndex,
                pageSize = pageSize
            )
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // آگهی‌های دولتی
    @GetMapping("/government")
    fun getGovernmentJobs(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int
    ): ServiceResponse<Page<JobPosting>> {
        return try {
            val data = service.getGovernmentJobs(pageIndex, pageSize)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // آگهی‌های یک دسته‌بندی
    @GetMapping("/category/{categoryId}")
    fun getByCategory(
        @PathVariable categoryId: Long,
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int
    ): ServiceResponse<Page<JobPosting>> {
        return try {
            val data = service.getByCategory(categoryId, pageIndex, pageSize)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // آگهی‌های یک شهر
    @GetMapping("/city/{cityId}")
    fun getByCity(
        @PathVariable cityId: Long,
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int
    ): ServiceResponse<Page<JobPosting>> {
        return try {
            val data = service.getByCity(cityId, pageIndex, pageSize)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جدیدترین آگهی‌ها
    @GetMapping("/latest")
    fun getLatest(): ServiceResponse<JobPosting> {
        return try {
            val data = service.getLatest()
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // تعداد کل آگهی‌ها
    @GetMapping("/count")
    fun getTotalCount(): ServiceResponse<Long> {
        return try {
            val count = service.getTotalCount()
            ServiceResponse(listOf(count), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // تعداد شهرها
    @GetMapping("/cities-count")
    fun getCitiesCount(): ServiceResponse<Long> {
        return try {
            val count = service.getCitiesCount()
            ServiceResponse(listOf(count), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // ========== EMPLOYER ENDPOINTS ==========
    
    @Autowired
    private lateinit var userService: com.iliyadev.springboot.services.customers.UserService
    
    @Autowired
    private lateinit var jwtUtil: com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
    
    // ایجاد آگهی جدید (برای کارفرما)
    @PostMapping
    fun createJob(
        @RequestBody job: JobPosting,
        request: jakarta.servlet.http.HttpServletRequest
    ): ServiceResponse<JobPosting> {
        return try {
            val username = com.iliyadev.springboot.utils.UserUtil.getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val newJob = job.copy(employer = user, createdAt = java.time.LocalDateTime.now())
                val data = service.create(newJob)
                ServiceResponse(listOf(data), HttpStatus.CREATED)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // به‌روزرسانی آگهی (برای کارفرما)
    @PutMapping("/{id}")
    fun updateJob(
        @PathVariable id: Long,
        @RequestBody job: JobPosting,
        request: jakarta.servlet.http.HttpServletRequest
    ): ServiceResponse<JobPosting> {
        return try {
            val username = com.iliyadev.springboot.utils.UserUtil.getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val existingJob = service.getById(id)
                if (existingJob != null && existingJob.employer?.id == user.id) {
                    val data = service.update(id, job)
                    if (data != null) {
                        ServiceResponse(listOf(data), HttpStatus.OK)
                    } else {
                        ServiceResponse(status = HttpStatus.NOT_FOUND, message = "آگهی یافت نشد")
                    }
                } else {
                    ServiceResponse(status = HttpStatus.FORBIDDEN, message = "شما مجاز به ویرایش این آگهی نیستید")
                }
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف آگهی (برای کارفرما)
    @DeleteMapping("/{id}")
    fun deleteJob(
        @PathVariable id: Long,
        request: jakarta.servlet.http.HttpServletRequest
    ): ServiceResponse<Boolean> {
        return try {
            val username = com.iliyadev.springboot.utils.UserUtil.getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val existingJob = service.getById(id)
                if (existingJob != null && existingJob.employer?.id == user.id) {
                    val success = service.delete(id)
                    if (success) {
                        ServiceResponse(listOf(true), HttpStatus.OK)
                    } else {
                        ServiceResponse(status = HttpStatus.NOT_FOUND, message = "آگهی یافت نشد")
                    }
                } else {
                    ServiceResponse(status = HttpStatus.FORBIDDEN, message = "شما مجاز به حذف این آگهی نیستید")
                }
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}

// ========== EMPLOYER CONTROLLER ==========
@RestController
@RequestMapping("api/employer")
@CrossOrigin
class EmployerController {
    
    @Autowired
    private lateinit var jobService: JobPostingService
    
    @Autowired
    private lateinit var applicationService: com.iliyadev.springboot.services.jobs.JobApplicationService
    
    @Autowired
    private lateinit var userService: com.iliyadev.springboot.services.customers.UserService
    
    @Autowired
    private lateinit var jwtUtil: com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
    
    // آگهی‌های کارفرما
    @GetMapping("/jobs")
    fun getMyJobs(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        request: jakarta.servlet.http.HttpServletRequest
    ): ServiceResponse<Page<JobPosting>> {
        return try {
            val username = com.iliyadev.springboot.utils.UserUtil.getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val data = jobService.getByEmployer(user.id, pageIndex, pageSize)
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // درخواست‌های دریافتی کارفرما (همه آگهی‌ها)
    @GetMapping("/applications")
    fun getMyApplications(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        request: jakarta.servlet.http.HttpServletRequest
    ): ServiceResponse<Page<com.iliyadev.springboot.models.jobs.JobApplication>> {
        return try {
            val username = com.iliyadev.springboot.utils.UserUtil.getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val data = applicationService.getByEmployer(user.id, pageIndex, pageSize)
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // دریافت اطلاعات شرکت (پروفایل کارفرما) - با فرمت سازگار با اندروید
    @GetMapping("/company")
    fun getCompanyInfo(request: jakarta.servlet.http.HttpServletRequest): ServiceResponse<Map<String, Any?>> {
        return try {
            val username = com.iliyadev.springboot.utils.UserUtil.getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                // تبدیل User به ساختار CompanyInfo سازگار با اندروید
                val companyInfo = mapOf(
                    "id" to user.id,
                    "name" to (user.companyName ?: ""),
                    "logo" to user.companyLogo,
                    "description" to user.companyDescription,
                    "website" to user.companyWebsite,
                    "size" to user.companySize,
                    "industry" to null,
                    "cityId" to null,
                    "cityName" to null,
                    "address" to null,
                    "establishedYear" to null
                )
                ServiceResponse(listOf(companyInfo), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // به‌روزرسانی اطلاعات شرکت - پشتیبانی از هر دو فرمت Map و CompanyInfo
    @PutMapping("/company")
    fun updateCompanyInfo(
        @RequestBody companyData: Map<String, Any?>,
        request: jakarta.servlet.http.HttpServletRequest
    ): ServiceResponse<Map<String, Any?>> {
        return try {
            val username = com.iliyadev.springboot.utils.UserUtil.getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                // پشتیبانی از هر دو فرمت فیلد name و companyName
                val companyName = (companyData["name"] as? String) 
                    ?: (companyData["companyName"] as? String) 
                    ?: user.companyName
                val companyDescription = (companyData["description"] as? String)
                    ?: (companyData["companyDescription"] as? String)
                    ?: user.companyDescription
                val companyWebsite = (companyData["website"] as? String)
                    ?: (companyData["companyWebsite"] as? String)
                    ?: user.companyWebsite
                val companySize = (companyData["size"] as? String)
                    ?: (companyData["companySize"] as? String)
                    ?: user.companySize
                val companyLogo = (companyData["logo"] as? String)
                    ?: (companyData["companyLogo"] as? String)
                    ?: user.companyLogo
                
                val updatedUser = user.copy(
                    companyName = companyName,
                    companyDescription = companyDescription,
                    companyWebsite = companyWebsite,
                    companySize = companySize,
                    companyLogo = companyLogo
                )
                val savedUser = userService.update(updatedUser, username)
                
                // برگرداندن پاسخ در فرمت CompanyInfo
                if (savedUser != null) {
                    val responseInfo = mapOf(
                        "id" to savedUser.id,
                        "name" to (savedUser.companyName ?: ""),
                        "logo" to savedUser.companyLogo,
                        "description" to savedUser.companyDescription,
                        "website" to savedUser.companyWebsite,
                        "size" to savedUser.companySize,
                        "industry" to null,
                        "cityId" to null,
                        "cityName" to null,
                        "address" to null,
                        "establishedYear" to null
                    )
                    ServiceResponse(listOf(responseInfo), HttpStatus.OK)
                } else {
                    ServiceResponse(status = HttpStatus.BAD_REQUEST, message = "خطا در ذخیره اطلاعات شرکت")
                }
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
