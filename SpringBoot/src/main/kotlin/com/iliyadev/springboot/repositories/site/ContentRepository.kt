package com.iliyadev.springboot.repositories.site

import com.iliyadev.springboot.models.site.Content
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface ContentRepository: PagingAndSortingRepository<Content,Long>, CrudRepository<Content, Long> {
    override fun findAll(): List<Content?>
    fun findFirstByTitle(title: String): Content?
}