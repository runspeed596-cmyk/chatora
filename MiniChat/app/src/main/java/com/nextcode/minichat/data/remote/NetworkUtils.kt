package com.nextcode.minichat.data.remote

import android.content.Context
import com.nextcode.minichat.R
import com.google.gson.Gson
import retrofit2.Response

object NetworkUtils {
    private val gson = Gson()

    fun <T> parseError(response: Response<T>): String {
        return parseErrorResponse(response).message
    }

    fun <T> getErrorCode(response: Response<T>): String? {
        return parseErrorResponse(response).code
    }

    fun <T> parseErrorResponse(response: Response<T>): ErrorResult {
        return try {
            val errorBody = response.errorBody()?.string()
            val apiResponse = gson.fromJson(errorBody, ApiResponse::class.java)
            ErrorResult(
                code = apiResponse?.error?.code,
                message = apiResponse?.error?.message ?: apiResponse?.message ?: "Error: ${response.code()}"
            )
        } catch (e: Exception) {
            ErrorResult(null, "Network error: ${response.code()}")
        }
    }

    fun getErrorMessageFromException(context: Context, e: Throwable): String {
        return if (e is ApiException) {
            ErrorMapper.mapCodeToMessage(context, e.code)
        } else {
            e.message ?: context.getString(R.string.error_network)
        }
    }
}

data class ErrorResult(val code: String?, val message: String)

class ApiException(val code: String?, message: String) : Exception(message)
