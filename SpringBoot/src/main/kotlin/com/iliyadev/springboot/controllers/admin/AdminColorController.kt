package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.Products.Color
import com.iliyadev.springboot.services.product.ColorService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/color")
class AdminColorController {

    @Autowired
    private lateinit var service: ColorService

    @PostMapping("")
    fun create(@RequestBody color: Color): ServiceResponse<Color> {
        return try {
            val created = service.create(color)
            ServiceResponse(listOf(created), HttpStatus.CREATED, "Color created successfully")
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to create color")
        }
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody color: Color): ServiceResponse<Color> {
        return try {
            val updated = service.update(id, color) ?: throw NotfoundExceptions("Color not found")
            ServiceResponse(listOf(updated), HttpStatus.OK, "Color updated successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to update color")
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ServiceResponse<Nothing> {
        return try {
            val deleted = service.delete(id)
            if (!deleted) throw NotfoundExceptions("Color not found")
            ServiceResponse(status = HttpStatus.OK, message = "Color deleted successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to delete color")
        }
    }
}
