package com.iliyadev.springboot.repositories.products

import com.iliyadev.springboot.models.Products.ProductImage
import org.springframework.data.repository.CrudRepository

interface ProductImageRepository : CrudRepository<ProductImage, Long> {
    fun findByProductId(productId: Long): List<ProductImage>
    fun findByProductIdAndIsPrimary(productId: Long, isPrimary: Boolean): ProductImage?
}
