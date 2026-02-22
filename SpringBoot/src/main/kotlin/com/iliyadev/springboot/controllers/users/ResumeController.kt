package com.iliyadev.springboot.controllers.users

import com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
import com.iliyadev.springboot.models.users.Resume
import com.iliyadev.springboot.services.users.ResumeService
import com.iliyadev.springboot.services.customers.UserService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.UserUtil.Companion.getCurrentUsername
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("api/resume")
@CrossOrigin
class ResumeController {
    
    @Autowired
    private lateinit var service: ResumeService
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var jwtUtil: JwtTokenUtils
    
    // آپلود رزومه
    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        request: HttpServletRequest
    ): ServiceResponse<Resume> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val data = service.upload(file, user.id)
                if (data != null) {
                    ServiceResponse(listOf(data), HttpStatus.OK)
                } else {
                    ServiceResponse(status = HttpStatus.BAD_REQUEST, message = "خطا در آپلود فایل")
                }
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // دریافت رزومه‌های کاربر
    @GetMapping
    fun getMyResumes(request: HttpServletRequest): ServiceResponse<Resume> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val data = service.getByUser(user.id)
                ServiceResponse(data, HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف رزومه
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, request: HttpServletRequest): ServiceResponse<Boolean> {
        return try {
            val success = service.delete(id)
            if (success) {
                ServiceResponse(listOf(true), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "رزومه یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // ========== RESUME PROFILE ENDPOINTS ==========
    
    // دریافت پروفایل رزومه - با فرمت سازگار با اندروید
    @GetMapping("/profile")
    fun getProfile(request: HttpServletRequest): ServiceResponse<Map<String, Any?>> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                // پارس کردن نام کامل به firstName و lastName
                val nameParts = (user.fullName ?: "").split(" ", limit = 2)
                val firstName = nameParts.getOrElse(0) { "" }
                val lastName = nameParts.getOrElse(1) { "" }
                
                // ساخت ساختار ResumeProfile سازگار با اندروید
                val profileData = mapOf(
                    "id" to user.id,
                    "userId" to user.id,
                    "personalInfo" to mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "gender" to "ANY",
                        "maritalStatus" to "SINGLE",
                        "militaryServiceStatus" to null,
                        "birthDay" to null,
                        "birthMonth" to null,
                        "birthYear" to null,
                        "cityId" to null,
                        "cityName" to null,
                        "neighborhood" to null,
                        "landlinePhone" to user.phone,
                        "isForeignNational" to false,
                        "hasDisability" to false,
                        "profileImageUrl" to user.profileImage
                    ),
                    "jobPreferences" to mapOf(
                        "expectedSalary" to null,
                        "jobInterests" to emptyList<Long>(),
                        "jobInterestNames" to emptyList<String>()
                    ),
                    "educations" to emptyList<Any>(),
                    "workExperiences" to emptyList<Any>(),
                    "softwareSkills" to emptyList<Any>(),
                    "additionalSkills" to emptyList<Any>(),
                    "languages" to emptyList<Any>(),
                    "hasNoWorkExperience" to false,
                    "isCompleted" to (user.skills != null || user.education != null),
                    "lastUpdatedAt" to null
                )
                ServiceResponse(listOf(profileData), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // ذخیره پروفایل رزومه
    @PostMapping("/profile")
    fun saveProfile(
        @RequestBody profileData: Map<String, Any?>,
        request: HttpServletRequest
    ): ServiceResponse<Map<String, Any?>> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                // استخراج اطلاعات شخصی - پشتیبانی از هر دو فرمت
                val personalInfo = profileData["personalInfo"] as? Map<*, *>
                val firstName = (personalInfo?.get("firstName") as? String) 
                    ?: (profileData["firstName"] as? String) 
                    ?: ""
                val lastName = (personalInfo?.get("lastName") as? String) 
                    ?: (profileData["lastName"] as? String) 
                    ?: ""
                val phone = (personalInfo?.get("landlinePhone") as? String) 
                    ?: (profileData["phone"] as? String) 
                    ?: user.phone
                
                val skills = profileData["skills"] as? String ?: user.skills
                val experience = profileData["experience"] as? String ?: user.experience
                val education = profileData["education"] as? String ?: user.education
                val bio = profileData["bio"] as? String ?: user.bio
                
                val updatedUser = user.copy(
                    fullName = "$firstName $lastName".trim(),
                    skills = skills,
                    experience = experience,
                    education = education,
                    bio = bio,
                    phone = phone
                )
                
                val saved = userService.update(updatedUser, username)
                if (saved != null) {
                    // برگرداندن پاسخ در فرمت ResumeProfile
                    val nameParts = (saved.fullName ?: "").split(" ", limit = 2)
                    val responseData = mapOf(
                        "id" to saved.id,
                        "userId" to saved.id,
                        "personalInfo" to mapOf(
                            "firstName" to nameParts.getOrElse(0) { "" },
                            "lastName" to nameParts.getOrElse(1) { "" },
                            "gender" to "ANY",
                            "maritalStatus" to "SINGLE",
                            "militaryServiceStatus" to null,
                            "birthDay" to null,
                            "birthMonth" to null,
                            "birthYear" to null,
                            "cityId" to null,
                            "cityName" to null,
                            "neighborhood" to null,
                            "landlinePhone" to saved.phone,
                            "isForeignNational" to false,
                            "hasDisability" to false,
                            "profileImageUrl" to saved.profileImage
                        ),
                        "jobPreferences" to mapOf(
                            "expectedSalary" to null,
                            "jobInterests" to emptyList<Long>(),
                            "jobInterestNames" to emptyList<String>()
                        ),
                        "educations" to emptyList<Any>(),
                        "workExperiences" to emptyList<Any>(),
                        "softwareSkills" to emptyList<Any>(),
                        "additionalSkills" to emptyList<Any>(),
                        "languages" to emptyList<Any>(),
                        "hasNoWorkExperience" to false,
                        "isCompleted" to true,
                        "lastUpdatedAt" to null
                    )
                    ServiceResponse(listOf(responseData), HttpStatus.OK)
                } else {
                    ServiceResponse(status = HttpStatus.BAD_REQUEST, message = "خطا در ذخیره پروفایل")
                }
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}

