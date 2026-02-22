package com.chatora.app.data.remote

import com.chatora.app.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface PaymentApiService {
    @GET("api/subscription/plans")
    suspend fun getPlans(): Response<List<SubscriptionPlanDto>>

    @GET("api/subscription/status")
    suspend fun getStatus(): Response<SubscriptionStatusDto>

    @POST("api/payment/create")
    suspend fun createPayment(@Body request: CreatePaymentRequestDto): Response<CreatePaymentResponseDto>

    @GET("api/payment/status/{id}")
    suspend fun getPaymentStatus(@Path("id") id: String): Response<PaymentStatusDto>
}
