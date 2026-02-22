package com.iliyadev.springboot.services.product

import com.iliyadev.springboot.models.Products.Color
import com.iliyadev.springboot.repositories.products.ColorRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ColorService {
    @Autowired
    lateinit var repository: ColorRepository

    fun getById(id: Long): Color? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun getAll(): List<Color?> {
        return repository.findAll()
    }
    fun getAllCount(): Long {
        return repository.count()
    }
    
    // Admin methods
    fun create(color: Color): Color {
        return repository.save(color)
    }
    
    fun update(id: Long, color: Color): Color? {
        if (!repository.existsById(id)) return null
        val updated = color.copy(id = id)
        return repository.save(updated)
    }
    
    fun delete(id: Long): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }
}