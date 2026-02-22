package com.iliyadev.springboot.services.locations

import com.iliyadev.springboot.models.locations.City
import com.iliyadev.springboot.repositories.locations.CityRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CityService {
    
    @Autowired
    private lateinit var repository: CityRepository
    
    fun getAll(): List<City> = repository.findAll()
    
    fun getById(id: Long): City? = repository.findById(id).orElse(null)
    
    fun getByProvince(provinceId: Long): List<City> = repository.findByProvinceId(provinceId)
    
    fun search(name: String): List<City> = repository.findByNameContainingIgnoreCase(name)
    
    fun create(city: City): City = repository.save(city)
    
    fun update(id: Long, city: City): City? {
        return if (repository.existsById(id)) {
            repository.save(city.copy(id = id))
        } else null
    }
    
    fun delete(id: Long): Boolean {
        return if (repository.existsById(id)) {
            repository.deleteById(id)
            true
        } else false
    }
}
