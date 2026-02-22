package com.iliyadev.springboot.controllers.admin

import com.iliyadev.springboot.models.Products.Product
import com.iliyadev.springboot.models.Products.ProductImage
import com.iliyadev.springboot.services.product.ProductImageService
import com.iliyadev.springboot.services.product.ProductService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("api/admin/product")
class AdminProductController {

    @Autowired
    private lateinit var service: ProductService
    
    @Autowired
    private lateinit var imageService: ProductImageService

    @PostMapping("")
    fun create(@RequestBody product: Product): ServiceResponse<Product> {
        return try {
            val created = service.create(product)
            ServiceResponse(listOf(created), HttpStatus.CREATED, "Product created successfully")
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to create product")
        }
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody product: Product): ServiceResponse<Product> {
        return try {
            val updated = service.update(id, product) ?: throw NotfoundExceptions("Product not found")
            ServiceResponse(listOf(updated), HttpStatus.OK, "Product updated successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to update product")
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ServiceResponse<Nothing> {
        return try {
            val deleted = service.delete(id)
            if (!deleted) throw NotfoundExceptions("Product not found")
            ServiceResponse(status = HttpStatus.OK, message = "Product deleted successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to delete product")
        }
    }

    @PutMapping("/{id}/stock")
    fun updateStock(@PathVariable id: Long, @RequestParam quantity: Int): ServiceResponse<Product> {
        return try {
            val updated = service.updateStock(id, quantity) ?: throw NotfoundExceptions("Product not found")
            ServiceResponse(listOf(updated), HttpStatus.OK, "Stock updated successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to update stock")
        }
    }

    @PostMapping("/{id}/images")
    fun uploadImage(@PathVariable id: Long, @RequestParam("file") file: MultipartFile, @RequestParam(defaultValue = "false") isPrimary: Boolean): ServiceResponse<ProductImage> {
        return try {
            val product = service.getById(id) ?: throw NotfoundExceptions("Product not found")
            val imageUrl = imageService.uploadImage(file)
            val productImage = ProductImage(imageUrl = imageUrl, isPrimary = isPrimary, product = product)
            val saved = imageService.saveProductImage(productImage)
            ServiceResponse(listOf(saved), HttpStatus.CREATED, "Image uploaded successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to upload image")
        }
    }

    @DeleteMapping("/image/{imageId}")
    fun deleteImage(@PathVariable imageId: Long): ServiceResponse<Nothing> {
        return try {
            val deleted = imageService.deleteImage(imageId)
            if (!deleted) throw NotfoundExceptions("Image not found")
            ServiceResponse(status = HttpStatus.OK, message = "Image deleted successfully")
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message ?: "Failed to delete image")
        }
    }
}
