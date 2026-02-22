package com.iliyadev.springboot.services.site

import com.iliyadev.springboot.models.site.Blog
import com.iliyadev.springboot.repositories.site.BlogRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class BlogService {
    @Autowired
    lateinit var repository: BlogRepository

    fun getById(id: Long): Blog? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun getAll(): List<Blog?> {
        return repository.findAll()
    }
    fun getAll(pageIndex: Int, pageSize: Int): List<Blog> {
        val pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by("id"))
        return repository.findAll(pageRequest).toList()
    }
    fun getAllCount(): Long {
        return repository.count()
    }
}