package com.iliyadev.springboot.models.invoices

import com.fasterxml.jackson.annotation.JsonIgnore
import com.iliyadev.springboot.models.Products.Product
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
data class InvoiceItem (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @ManyToOne()
    @JoinColumn(name = "product_id")
    var product: Product? = null,
    var quantity: Int = 0,
    var unitPrice: Long = 0,

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "invoice_id")
    var invoice: Invoice? = null,
)