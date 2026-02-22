package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.Products.ProductCategory
import com.iliyadev.springboot.services.product.ProductCategoryService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/category")
class AdminCategoryController {

    @Autowired
    private lateinit var service: ProductCategoryService

    @PostMapping("")
    fun create(@RequestBody category: ProductCategory): ServiceResponse<ProductCategory> {
        return try {
            val created = service.create(category)
            ServiceResponse(listOf(created), HttpStatus.CREATED, "Category created successfully")
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to create category")
        }
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody category: ProductCategory): ServiceResponse<ProductCategory> {
        return try {
            val updated = service.update(id, category) ?: throw NotfoundExceptions("Category not found")
            ServiceResponse(listOf(updated), HttpStatus.OK, "Category updated successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to update category")
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ServiceResponse<Nothing> {
        return try {
            val deleted = service.delete(id)
            if (!deleted) throw NotfoundExceptions("Category not found")
            ServiceResponse(status = HttpStatus.OK, message = "Category deleted successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to delete category")
        }
    }
}
