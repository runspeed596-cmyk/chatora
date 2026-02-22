package com.iliyadev.springboot.services.InvoiceItems

import com.iliyadev.springboot.models.invoices.InvoiceItem
import com.iliyadev.springboot.repositories.invoices.InvoiceItemRepository
import com.iliyadev.springboot.services.product.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class InvoiceItemService {
    @Autowired
    private lateinit var repository: InvoiceItemRepository

    @Autowired
    private lateinit var productService: ProductService

    fun getById(id: Long): InvoiceItem? {
        val data = repository.findById(id)
        if (data.isEmpty) return null
        return data.get()
    }

    fun getAllByInvoiceId(invoiceId: Int): List<InvoiceItem> {
        return repository.findAllByInvoiceId(invoiceId).toList()
    }

    fun addItem(invoiceItem: InvoiceItem): InvoiceItem {
        if (invoiceItem.quantity <= 0)
            throw Exception("invalid quantity")
        if (invoiceItem.product == null || invoiceItem.product!!.id <= 0)
            throw Exception("product id is invalid")

        val productPrice = productService.getPriceById(invoiceItem.product!!.id)
        invoiceItem.unitPrice = productPrice!!
        return repository.save(invoiceItem)
    }
}