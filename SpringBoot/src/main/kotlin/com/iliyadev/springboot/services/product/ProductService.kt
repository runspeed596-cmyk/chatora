package com.iliyadev.springboot.services.product

import com.iliyadev.springboot.models.Products.Product
import com.iliyadev.springboot.repositories.products.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class ProductService {
    @Autowired
    lateinit var repository: ProductRepository

    fun getById(id: Long): Product? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun getAll(): List<Product?> {
        return repository.findAll()
    }
    fun getAll(pageIndex: Int, pageSize: Int): List<Product> {
        val pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by("id"))
        return repository.findAll(pageRequest).toList()
    }

    fun getNewProducts(): List<Product> {
        return repository.findTop6ByOrderByAddDateDesc()
    }
    fun getPopularProducts(): List<Product> {
        return repository.findTop6ByOrderByVisitCountDesc()
    }
    fun getAllCount(): Long {
        return repository.count()
    }
    fun getPriceById(id: Long): Long? {
        return repository.findFirstPriceById(id)
    }
    
    // Admin methods
    fun create(product: Product): Product {
        return repository.save(product)
    }
    
    fun update(id: Long, product: Product): Product? {
        val existing = getById(id) ?: return null
        val updated = product.copy(id = id)
        return repository.save(updated)
    }
    
    fun delete(id: Long): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }
    
    fun updateStock(id: Long, quantity: Int): Product? {
        val product = getById(id) ?: return null
        val updated = product.copy(stockQuantity = quantity)
        return repository.save(updated)
    }
}