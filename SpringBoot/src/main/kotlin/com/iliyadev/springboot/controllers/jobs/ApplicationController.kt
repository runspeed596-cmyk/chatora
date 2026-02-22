package com.iliyadev.springboot.controllers.jobs

import com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
import com.iliyadev.springboot.models.jobs.JobApplication
import com.iliyadev.springboot.services.customers.UserService
import com.iliyadev.springboot.services.jobs.JobApplicationService
import com.iliyadev.springboot.services.jobs.JobPostingService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.UserUtil.Companion.getCurrentUsername
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/applications")
@CrossOrigin
class ApplicationController {
    
    @Autowired
    private lateinit var service: JobApplicationService
    
    @Autowired
    private lateinit var jobService: JobPostingService
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var jwtUtil: JwtTokenUtils
    
    // ارسال درخواست کار
    @PostMapping("/apply/{jobId}")
    fun apply(
        @PathVariable jobId: Long,
        @RequestParam(required = false) coverLetter: String?,
        request: HttpServletRequest
    ): ServiceResponse<JobApplication> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            val job = jobService.getById(jobId)
            
            if (user == null) {
                return ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
            if (job == null) {
                return ServiceResponse(status = HttpStatus.NOT_FOUND, message = "آگهی یافت نشد")
            }
            
            val application = JobApplication(
                applicant = user,
                job = job,
                coverLetter = coverLetter
            )
            
            val data = service.apply(application)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.BAD_REQUEST, message = "شما قبلاً برای این آگهی درخواست داده‌اید")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // درخواست‌های من (برای کارجو)
    @GetMapping("/my-applications")
    fun getMyApplications(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        request: HttpServletRequest
    ): ServiceResponse<Page<JobApplication>> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val data = service.getByApplicant(user.id, pageIndex, pageSize)
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // درخواست‌های یک آگهی (برای کارفرما)
    @GetMapping("/job/{jobId}")
    fun getJobApplications(
        @PathVariable jobId: Long,
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "10") pageSize: Int
    ): ServiceResponse<Page<JobApplication>> {
        return try {
            val data = service.getByJob(jobId, pageIndex, pageSize)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
