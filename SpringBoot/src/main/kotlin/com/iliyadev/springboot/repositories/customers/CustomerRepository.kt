package com.iliyadev.springboot.repositories.customers

import com.iliyadev.springboot.models.customers.Customer
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface CustomerRepository: PagingAndSortingRepository<Customer,Long>, CrudRepository<Customer, Long> {
}