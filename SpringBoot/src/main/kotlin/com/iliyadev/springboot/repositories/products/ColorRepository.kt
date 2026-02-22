package com.iliyadev.springboot.repositories.products

import com.iliyadev.springboot.models.Products.Color
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface ColorRepository: PagingAndSortingRepository<Color,Long>, CrudRepository<Color, Long> {
    override fun findAll(): List<Color?>
}