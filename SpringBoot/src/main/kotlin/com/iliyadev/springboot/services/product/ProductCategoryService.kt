package com.iliyadev.springboot.services.product

import com.iliyadev.springboot.models.Products.ProductCategory
import com.iliyadev.springboot.repositories.products.ProductCategoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProductCategoryService {
    @Autowired
    lateinit var repository: ProductCategoryRepository

    fun getById(id: Long): ProductCategory? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun getAll(): List<ProductCategory?> {
        return repository.findAll()
    }
    fun getAllCount(): Long {
        return repository.count()
    }
    
    // Admin methods
    fun create(category: ProductCategory): ProductCategory {
        return repository.save(category)
    }
    
    fun update(id: Long, category: ProductCategory): ProductCategory? {
        if (!repository.existsById(id)) return null
        val updated = category.copy(id = id)
        return repository.save(updated)
    }
    
    fun delete(id: Long): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }
}