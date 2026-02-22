package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.jobs.JobCategory
import com.iliyadev.springboot.services.jobs.JobCategoryService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/categories")
@CrossOrigin
class AdminJobCategoryController {
    
    @Autowired
    private lateinit var service: JobCategoryService
    
    // لیست دسته‌بندی‌ها
    @GetMapping
    fun getAll(): ServiceResponse<JobCategory> {
        return try {
            val data = service.getAll()
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // ایجاد دسته‌بندی
    @PostMapping
    fun create(@RequestBody category: JobCategory): ServiceResponse<JobCategory> {
        return try {
            val data = service.create(category)
            ServiceResponse(listOf(data), HttpStatus.CREATED)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // به‌روزرسانی دسته‌بندی
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody category: JobCategory): ServiceResponse<JobCategory> {
        return try {
            val data = service.update(id, category)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "دسته‌بندی یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف دسته‌بندی
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ServiceResponse<Boolean> {
        return try {
            val success = service.delete(id)
            if (success) {
                ServiceResponse(listOf(true), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "دسته‌بندی یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
