package com.iliyadev.springboot.services.locations

import com.iliyadev.springboot.models.locations.Province
import com.iliyadev.springboot.repositories.locations.ProvinceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProvinceService {
    
    @Autowired
    private lateinit var repository: ProvinceRepository
    
    fun getAll(): List<Province> = repository.findAll()
    
    fun getById(id: Long): Province? = repository.findById(id).orElse(null)
    
    fun search(name: String): List<Province> = repository.findByNameContainingIgnoreCase(name)
    
    fun create(province: Province): Province = repository.save(province)
    
    fun update(id: Long, province: Province): Province? {
        return if (repository.existsById(id)) {
            repository.save(province.copy(id = id))
        } else null
    }
    
    fun delete(id: Long): Boolean {
        return if (repository.existsById(id)) {
            repository.deleteById(id)
            true
        } else false
    }
}
