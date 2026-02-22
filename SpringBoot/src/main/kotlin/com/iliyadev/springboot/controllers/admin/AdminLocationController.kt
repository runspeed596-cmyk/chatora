package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.locations.City
import com.iliyadev.springboot.models.locations.Province
import com.iliyadev.springboot.services.locations.CityService
import com.iliyadev.springboot.services.locations.ProvinceService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/locations")
@CrossOrigin
class AdminLocationController {
    
    @Autowired
    private lateinit var provinceService: ProvinceService
    
    @Autowired
    private lateinit var cityService: CityService
    
    // ایجاد استان
    @PostMapping("/provinces")
    fun createProvince(@RequestBody province: Province): ServiceResponse<Province> {
        return try {
            val data = provinceService.create(province)
            ServiceResponse(listOf(data), HttpStatus.CREATED)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // به‌روزرسانی استان
    @PutMapping("/provinces/{id}")
    fun updateProvince(@PathVariable id: Long, @RequestBody province: Province): ServiceResponse<Province> {
        return try {
            val data = provinceService.update(id, province)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "استان یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف استان
    @DeleteMapping("/provinces/{id}")
    fun deleteProvince(@PathVariable id: Long): ServiceResponse<Boolean> {
        return try {
            val success = provinceService.delete(id)
            if (success) {
                ServiceResponse(listOf(true), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "استان یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // ایجاد شهر
    @PostMapping("/cities")
    fun createCity(@RequestBody city: City): ServiceResponse<City> {
        return try {
            val data = cityService.create(city)
            ServiceResponse(listOf(data), HttpStatus.CREATED)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // به‌روزرسانی شهر
    @PutMapping("/cities/{id}")
    fun updateCity(@PathVariable id: Long, @RequestBody city: City): ServiceResponse<City> {
        return try {
            val data = cityService.update(id, city)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "شهر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // حذف شهر
    @DeleteMapping("/cities/{id}")
    fun deleteCity(@PathVariable id: Long): ServiceResponse<Boolean> {
        return try {
            val success = cityService.delete(id)
            if (success) {
                ServiceResponse(listOf(true), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "شهر یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
