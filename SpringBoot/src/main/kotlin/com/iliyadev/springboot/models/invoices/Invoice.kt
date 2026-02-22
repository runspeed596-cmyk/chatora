package com.iliyadev.springboot.models.invoices

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.models.enums.InvoiceStatus
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

@Entity
data class Invoice (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    var status: InvoiceStatus = InvoiceStatus.NotPayed,
    var addDate: String = "",
    var paymentDate: String ="",

    //@JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @OneToMany(mappedBy = "invoice")
    val items: Set<InvoiceItem>? = null,

    @OneToMany(mappedBy = "invoice")
    var transactions: Set<Transaction>? = null
)