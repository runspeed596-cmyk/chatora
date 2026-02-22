package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.Products.Size
import com.iliyadev.springboot.services.product.SizeService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/size")
class AdminSizeController {

    @Autowired
    private lateinit var service: SizeService

    @PostMapping("")
    fun create(@RequestBody size: Size): ServiceResponse<Size> {
        return try {
            val created = service.create(size)
            ServiceResponse(listOf(created), HttpStatus.CREATED, "Size created successfully")
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to create size")
        }
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody size: Size): ServiceResponse<Size> {
        return try {
            val updated = service.update(id, size) ?: throw NotfoundExceptions("Size not found")
            ServiceResponse(listOf(updated), HttpStatus.OK, "Size updated successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to update size")
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ServiceResponse<Nothing> {
        return try {
            val deleted = service.delete(id)
            if (!deleted) throw NotfoundExceptions("Size not found")
            ServiceResponse(status = HttpStatus.OK, message = "Size deleted successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to delete size")
        }
    }
}
