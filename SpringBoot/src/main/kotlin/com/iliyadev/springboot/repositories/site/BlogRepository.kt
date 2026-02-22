package com.iliyadev.springboot.repositories.site

import com.iliyadev.springboot.models.site.Blog
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface BlogRepository: PagingAndSortingRepository<Blog,Long>, CrudRepository<Blog, Long> {
    override fun findAll(): List<Blog>
}