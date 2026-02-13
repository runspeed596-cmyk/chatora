package com.nextcode.minichat.data.remote

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: ApiError? = null,
    val timestamp: String? = null
)

data class ApiError(
    val code: String,
    val message: String,
    val details: Any? = null
)
