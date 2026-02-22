package com.iliyadev.springboot.viewmodels

data class VerifyResponseViewModel(
    val status: String,
    val referenceId: String,
    val invoiceNumber: Long,
    val code: Int
)