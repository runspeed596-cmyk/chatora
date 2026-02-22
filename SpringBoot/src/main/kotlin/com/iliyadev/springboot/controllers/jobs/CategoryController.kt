package com.iliyadev.springboot.controllers.jobs

import com.iliyadev.springboot.models.jobs.JobCategory
import com.iliyadev.springboot.services.jobs.JobCategoryService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/categories")
@CrossOrigin
class CategoryController {
    
    @Autowired
    private lateinit var service: JobCategoryService
    
    // لیست همه دسته‌بندی‌ها
    @GetMapping
    fun getAll(): ServiceResponse<JobCategory> {
        return try {
            val data = service.getAll()
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جزئیات دسته‌بندی
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ServiceResponse<JobCategory> {
        return try {
            val data = service.getById(id)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "دسته‌بندی یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جستجوی دسته‌بندی
    @GetMapping("/search")
    fun search(@RequestParam name: String): ServiceResponse<JobCategory> {
        return try {
            val data = service.search(name)
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
