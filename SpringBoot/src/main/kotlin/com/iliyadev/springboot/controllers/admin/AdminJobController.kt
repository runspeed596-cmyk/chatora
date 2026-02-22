package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.jobs.JobPosting
import com.iliyadev.springboot.services.jobs.JobPostingService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/jobs")
@CrossOrigin
class AdminJobController {
    
    @Autowired
    private lateinit var service: JobPostingService
    
    // لیست آگهی‌ها برای ادمین
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "10") pageSize: Int
    ): ServiceResponse<Page<JobPosting>> {
        return try {
            val data = service.getAll(pageIndex, pageSize)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // ایجاد آگهی
    @PostMapping
    fun create(@RequestBody job: JobPosting): ServiceResponse<JobPosting> {
        return try {
            val data = service.create(job)
            ServiceResponse(listOf(data), HttpStatus.CREATED)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // به‌روزرسانی آگهی
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody job: JobPosting): ServiceResponse<JobPosting> {
        return try {
            val data = service.update(id, job)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "آگهی یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف آگهی
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ServiceResponse<Boolean> {
        return try {
            val success = service.delete(id)
            if (success) {
                ServiceResponse(listOf(true), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "آگهی یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // غیرفعال کردن آگهی
    @PutMapping("/{id}/deactivate")
    fun deactivate(@PathVariable id: Long): ServiceResponse<JobPosting> {
        return try {
            val data = service.deactivate(id)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "آگهی یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
