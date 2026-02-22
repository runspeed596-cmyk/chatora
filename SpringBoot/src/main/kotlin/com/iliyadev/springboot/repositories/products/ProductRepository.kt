package com.iliyadev.springboot.repositories.products

import com.iliyadev.springboot.models.Products.Product
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface ProductRepository: PagingAndSortingRepository<Product,Long>, CrudRepository<Product, Long> {
    override fun findAll(): List<Product?>
    fun findTop6ByOrderByAddDateDesc() : List<Product>
    fun findTop6ByOrderByVisitCountDesc(): List<Product>
    @Query("select price from  Product where id = :id")
    fun findFirstPriceById(id: Long): Long?
}