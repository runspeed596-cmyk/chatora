package com.iliyadev.springboot.controllers.jobs

import com.iliyadev.springboot.models.jobs.CooperationType
import com.iliyadev.springboot.services.jobs.CooperationTypeService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/cooperation-types")
@CrossOrigin
class CooperationTypeController {
    
    @Autowired
    private lateinit var service: CooperationTypeService
    
    // لیست همه انواع همکاری
    @GetMapping
    fun getAll(): ServiceResponse<CooperationType> {
        return try {
            val data = service.getAll()
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جزئیات نوع همکاری
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ServiceResponse<CooperationType> {
        return try {
            val data = service.getById(id)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "نوع همکاری یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
