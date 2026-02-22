package com.iliyadev.springboot.models.enums

enum class ApplicationStatus {
    PENDING,    // در انتظار بررسی
    REVIEWED,   // بررسی شده
    ACCEPTED,   // پذیرفته شده
    REJECTED    // رد شده
}
