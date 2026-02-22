package com.iliyadev.springboot.repositories.products

import com.iliyadev.springboot.models.Products.Size
import org.springframework.data.repository.CrudRepository

interface SizeRepository : CrudRepository<Size, Long> {
    override fun findAll(): List<Size>
}
