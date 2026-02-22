package com.iliyadev.springboot.repositories.products

import com.iliyadev.springboot.models.Products.ProductCategory
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface ProductCategoryRepository: PagingAndSortingRepository<ProductCategory,Long>, CrudRepository<ProductCategory, Long> {
    override fun findAll(): List<ProductCategory?>
}