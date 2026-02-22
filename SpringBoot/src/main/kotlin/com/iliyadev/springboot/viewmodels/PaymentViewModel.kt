package com.iliyadev.springboot.viewmodels

import com.iliyadev.springboot.models.invoices.InvoiceItem


data class PaymentViewModel(
    val user: UserViewModel,
    val items: List<InvoiceItem>
)