package com.iliyadev.springboot.repositories.invoices

import com.iliyadev.springboot.models.invoices.Transaction
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : PagingAndSortingRepository<Transaction, Long> {
    fun findByTransId(transId: String): Transaction?
}