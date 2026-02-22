package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.jobs.CooperationType
import com.iliyadev.springboot.services.jobs.CooperationTypeService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/cooperation-types")
@CrossOrigin
class AdminCooperationTypeController {
    
    @Autowired
    private lateinit var service: CooperationTypeService
    
    // لیست انواع همکاری
    @GetMapping
    fun getAll(): ServiceResponse<CooperationType> {
        return try {
            val data = service.getAll()
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // ایجاد نوع همکاری
    @PostMapping
    fun create(@RequestBody type: CooperationType): ServiceResponse<CooperationType> {
        return try {
            val data = service.create(type)
            ServiceResponse(listOf(data), HttpStatus.CREATED)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // به‌روزرسانی نوع همکاری
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody type: CooperationType): ServiceResponse<CooperationType> {
        return try {
            val data = service.update(id, type)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "نوع همکاری یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف نوع همکاری
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ServiceResponse<Boolean> {
        return try {
            val success = service.delete(id)
            if (success) {
                ServiceResponse(listOf(true), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "نوع همکاری یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
