package com.iliyadev.springboot.utils

import org.springframework.http.HttpStatus
import java.io.Serializable

data class ServiceResponse<T>(
    var data: List<T>? = null,
    var status: HttpStatus,
    var message: String = "",
    var totalCount: Long = 0,

): Serializable