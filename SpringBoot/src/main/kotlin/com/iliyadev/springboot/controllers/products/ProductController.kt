package com.iliyadev.springboot.controllers.products

import com.iliyadev.springboot.models.Products.Product
import com.iliyadev.springboot.services.product.ProductService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/product")
class ProductController {

    @Autowired
    private lateinit var service: ProductService

    @GetMapping("")
    fun getAll(@RequestParam pageSize: Int,@RequestParam pageIndex: Int): ServiceResponse<Product?> {
        return try {
            ServiceResponse(service.getAll(pageIndex,pageSize), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

    @GetMapping("/new")
        fun getNewProducts(): ServiceResponse<Product?> {
        return try {
            ServiceResponse(service.getNewProducts(), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

        @GetMapping("/popular")
    fun getPopularProducts(): ServiceResponse<Product?> {
        return try {
            ServiceResponse(service.getPopularProducts(), HttpStatus.OK)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ServiceResponse<Product> {
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