package com.iliyadev.springboot.services.product

import com.iliyadev.springboot.models.Products.Size
import com.iliyadev.springboot.repositories.products.SizeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SizeService {
    @Autowired
    lateinit var repository: SizeRepository

    fun getById(id: Long): Size? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun getAll(): List<Size> {
        return repository.findAll()
    }
    
    fun getAllCount(): Long {
        return repository.count()
    }
    
    // Admin methods
    fun create(size: Size): Size {
        return repository.save(size)
    }
    
    fun update(id: Long, size: Size): Size? {
        if (!repository.existsById(id)) return null
        val updated = size.copy(id = id)
        return repository.save(updated)
    }
    
    fun delete(id: Long): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }
}
