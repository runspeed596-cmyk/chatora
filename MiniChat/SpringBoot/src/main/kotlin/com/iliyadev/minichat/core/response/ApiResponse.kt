package com.iliyadev.minichat.core.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: ApiError? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(true, message, data)
        }

        fun error(code: String, message: String, details: Any? = null): ApiResponse<Nothing> {
            return ApiResponse(
                success = false,
                error = ApiError(code, message, details)
            )
        }
    }
}

data class ApiError(
    val code: String,
    val message: String,
    val details: Any? = null
)
