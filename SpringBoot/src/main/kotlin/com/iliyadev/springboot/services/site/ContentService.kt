package com.iliyadev.springboot.services.site

import com.iliyadev.springboot.models.site.Content
import com.iliyadev.springboot.repositories.site.ContentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ContentService {
    @Autowired
    lateinit var repository: ContentRepository

    fun getById(id: Long): Content? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun getAll(): List<Content?> {
        return repository.findAll()
    }
    fun delete(data: Content): Boolean {
        repository.delete(data)
        return true
    }

    fun getByTitle(title: String): Content? {
        return repository.findFirstByTitle(title)
    }
}