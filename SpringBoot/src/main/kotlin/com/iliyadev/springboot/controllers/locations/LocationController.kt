package com.iliyadev.springboot.controllers.locations

import com.iliyadev.springboot.models.locations.City
import com.iliyadev.springboot.models.locations.Province
import com.iliyadev.springboot.services.locations.CityService
import com.iliyadev.springboot.services.locations.ProvinceService
import com.iliyadev.springboot.utils.ServiceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/locations")
@CrossOrigin
class LocationController {
    
    @Autowired
    private lateinit var provinceService: ProvinceService
    
    @Autowired
    private lateinit var cityService: CityService
    
    // لیست همه استان‌ها
    @GetMapping("/provinces")
    fun getAllProvinces(): ServiceResponse<Province> {
        return try {
            val data = provinceService.getAll()
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جزئیات استان
    @GetMapping("/provinces/{id}")
    fun getProvinceById(@PathVariable id: Long): ServiceResponse<Province> {
        return try {
            val data = provinceService.getById(id)
            if (data != null) {
                ServiceResponse(listOf(data), HttpStatus.OK)
            } else {
                ServiceResponse(status = HttpStatus.NOT_FOUND, message = "استان یافت نشد")
            }
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // شهرهای یک استان
    @GetMapping("/provinces/{id}/cities")
    fun getCitiesByProvince(@PathVariable id: Long): ServiceResponse<City> {
        return try {
            val data = cityService.getByProvince(id)
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // لیست همه شهرها
    @GetMapping("/cities")
    fun getAllCities(): ServiceResponse<City> {
        return try {
            val data = cityService.getAll()
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
    
    // جستجوی شهر
    @GetMapping("/cities/search")
    fun searchCities(@RequestParam name: String): ServiceResponse<City> {
        return try {
            val data = cityService.search(name)
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Error")
        }
    }
}
