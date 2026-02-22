package com.iliyadev.springboot.controllers.users

import com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
import com.iliyadev.springboot.models.users.Notification
import com.iliyadev.springboot.services.users.NotificationService
import com.iliyadev.springboot.services.customers.UserService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.UserUtil.Companion.getCurrentUsername
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/notifications")
@CrossOrigin
class NotificationController {
    
    @Autowired
    private lateinit var service: NotificationService
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var jwtUtil: JwtTokenUtils
    
    // اعلان‌های کاربر
    @GetMapping
    fun getMyNotifications(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        request: HttpServletRequest
    ): ServiceResponse<Page<Notification>> {
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
    
    // تعداد اعلان‌های خوانده نشده
    @GetMapping("/unread-count")
    fun getUnreadCount(request: HttpServletRequest): ServiceResponse<Long> {
        return try {
            val username = getCurrentUsername(jwtUtil, request)
            val user = userService.getByUsername(username)
            if (user != null) {
                val count = service.countUnread(user.id)
                ServiceResponse(listOf(count), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.UNAUTHORIZED, message = "کاربر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // خوانده شده کردن اعلان
    @PutMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long): ServiceResponse<Notification> {
        return try {
            val data = service.markAsRead(id)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "اعلان یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
