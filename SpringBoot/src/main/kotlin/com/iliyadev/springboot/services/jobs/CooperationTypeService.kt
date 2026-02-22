package com.iliyadev.springboot.services.jobs

import com.iliyadev.springboot.models.jobs.CooperationType
import com.iliyadev.springboot.repositories.jobs.CooperationTypeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CooperationTypeService {
    
    @Autowired
    private lateinit var repository: CooperationTypeRepository
    
    fun getAll(): List<CooperationType> = repository.findAll()
    
    fun getById(id: Long): CooperationType? = repository.findById(id).orElse(null)
    
    fun create(type: CooperationType): CooperationType = repository.save(type)
    
    fun update(id: Long, type: CooperationType): CooperationType? {
        return if (repository.existsById(id)) {
            repository.save(type.copy(id = id))
        } else null
    }
    
    fun delete(id: Long): Boolean {
        return if (repository.existsById(id)) {
            repository.deleteById(id)
            true
        } else false
    }
}
