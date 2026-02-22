package com.iliyadev.springboot.services.product

import com.iliyadev.springboot.models.Products.ProductImage
import com.iliyadev.springboot.repositories.products.ProductImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class ProductImageService {
    @Autowired
    lateinit var repository: ProductImageRepository
    
    private val uploadDir = "uploads/products/"
    
    init {
        // Create upload directory if it doesn't exist
        File(uploadDir).mkdirs()
    }
    
    fun uploadImage(file: MultipartFile): String {
        val fileName = "${UUID.randomUUID()}_${file.originalFilename}"
        val filePath = Paths.get(uploadDir + fileName)
        Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
        return "/$uploadDir$fileName"
    }
    
    fun saveProductImage(productImage: ProductImage): ProductImage {
        return repository.save(productImage)
    }
    
    fun getImagesByProductId(productId: Long): List<ProductImage> {
        return repository.findByProductId(productId)
    }
    
    fun getPrimaryImage(productId: Long): ProductImage? {
        return repository.findByProductIdAndIsPrimary(productId, true)
    }
    
    fun deleteImage(imageId: Long): Boolean {
        val image = repository.findById(imageId)
        if (image.isEmpty) return false
        
        // Delete file from disk
        try {
            val file = File(image.get().imageUrl.removePrefix("/"))
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Log error but continue with database deletion
        }
        
        repository.deleteById(imageId)
        return true
    }
}
