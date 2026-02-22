package com.iliyadev.springboot.models.customers

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType

// مدل مشتری - بخش قدیمی (e-commerce)
// این مدل دیگر استفاده نمی‌شود و برای سازگاری با قبل نگهداری شده
@Entity
data class Customer (
    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    var firstName: String = "",
    var lastName: String = "",
    var address: String = "",
    var phone: String = "",
    var postalCode: String = ""
)