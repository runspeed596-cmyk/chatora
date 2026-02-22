package com.iliyadev.springboot.controllers.site

import com.iliyadev.springboot.models.site.Slider
import com.iliyadev.springboot.services.site.SliderService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/slider")
class SliderController {

    @Autowired
    private lateinit var service: SliderService

    @GetMapping("")
    fun getAll(): ServiceResponse<Slider> {
        return try {
            ServiceResponse(service.getAll(), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ServiceResponse<Slider> {
        return try {
            val data = service.getById(id) ?: throw NotfoundExceptions("data not found")
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }

    }


}