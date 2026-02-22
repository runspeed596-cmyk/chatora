package com.iliyadev.springboot.controllers.users

import com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
import com.iliyadev.springboot.models.users.Bookmark
import com.iliyadev.springboot.services.customers.UserService
import com.iliyadev.springboot.services.jobs.JobPostingService
import com.iliyadev.springboot.services.users.BookmarkService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.UserUtil.Companion.getCurrentUsername
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/bookmarks")
@CrossOrigin
class BookmarkController {
    
    @Autowired
    private lateinit var service: BookmarkService
    
    @Autowired
    private lateinit var jobService: JobPostingService
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var jwtUtil: JwtTokenUtils
    
    // نشان‌شده‌های من
    @GetMapping
    fun getMyBookmarks(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        request: HttpServletRequest
    ): ServiceResponse<Page<Bookmark>> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val data = service.getByUser(user.id, pageIndex, pageSize)
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // افزودن به نشان‌شده‌ها
    @PostMapping("/{jobId}")
    fun addBookmark(
        @PathVariable jobId: Long,
        request: HttpServletRequest
    ): ServiceResponse<Bookmark> {
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
            
            val data = service.add(user, job)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.BAD_REQUEST, message = "این آگهی قبلاً نشان شده است")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف از نشان‌شده‌ها
    @DeleteMapping("/{jobId}")
    fun removeBookmark(
        @PathVariable jobId: Long,
        request: HttpServletRequest
    ): ServiceResponse<Boolean> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            
            if (user == null) {
                return ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
            
            val success = service.remove(user.id, jobId)
            if (success) {
                ServiceResponse(listOf(true), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "این آگهی نشان نشده است")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // بررسی نشان‌شده بودن
    @GetMapping("/check/{jobId}")
    fun isBookmarked(
        @PathVariable jobId: Long,
        request: HttpServletRequest
    ): ServiceResponse<Boolean> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            
            if (user == null) {
                return ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
            
            val isBookmarked = service.isBookmarked(user.id, jobId)
            ServiceResponse(listOf(isBookmarked), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
