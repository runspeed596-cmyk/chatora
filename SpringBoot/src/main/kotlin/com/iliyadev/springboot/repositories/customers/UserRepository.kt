package com.iliyadev.springboot.repositories.customers

import com.iliyadev.springboot.models.customers.User
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface UserRepository: PagingAndSortingRepository<User,Long>, CrudRepository<User, Long> {
    fun findFirstByUsernameAndPassword(username: String, password: String): User?
    fun findFirstByUsername(username: String): User?
}