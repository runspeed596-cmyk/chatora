package com.iliyadev.springboot.repositories.invoices

import com.iliyadev.springboot.models.invoices.InvoiceItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface InvoiceItemRepository: PagingAndSortingRepository<InvoiceItem,Long>, CrudRepository<InvoiceItem, Long> {
    @Query("from InvoiceItem where invoice.id = :invoiceId")
    fun findAllByInvoiceId(invoiceId: Int): List<InvoiceItem>
}