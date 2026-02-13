package com.chatora.app.data.repositories

import com.chatora.app.data.models.*
import com.chatora.app.data.remote.PaymentApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: PaymentApiService
) {
    fun getPlans(): Flow<Result<List<SubscriptionPlanDto>>> = flow {
        try {
            val response = api.getPlans()
            if (response.isSuccessful) {
                emit(Result.success(response.body() ?: emptyList()))
            } else {
                emit(Result.failure(Exception("Failed to fetch plans: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getSubscriptionStatus(): Flow<Result<SubscriptionStatusDto>> = flow {
        try {
            val response = api.getStatus()
            if (response.isSuccessful) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Failed to fetch status: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun createPayment(plan: String): Result<CreatePaymentResponseDto> {
        return try {
            val response = api.createPayment(CreatePaymentRequestDto(plan))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create payment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
