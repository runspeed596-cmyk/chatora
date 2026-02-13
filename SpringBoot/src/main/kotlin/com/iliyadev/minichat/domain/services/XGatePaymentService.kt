package com.iliyadev.minichat.domain.services

import com.iliyadev.minichat.api.dtos.*
import com.iliyadev.minichat.domain.entities.SubscriptionPlan
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class XGatePaymentService(
    @Value("\${xgate.api.key}") private val apiKey: String,
    @Value("\${xgate.api.base-url:https://api.1xgate.com}") private val baseUrl: String,
    @Value("\${xgate.webhook.success-url}") private val successUrl: String,
    @Value("\${xgate.webhook.failed-url}") private val failedUrl: String
) {
    private val logger = LoggerFactory.getLogger(XGatePaymentService::class.java)
    private val restTemplate = RestTemplate()

    fun createPayment(userId: UUID, plan: SubscriptionPlan, orderId: String): CreatePaymentResponse {
        val url = "$baseUrl/v1/integrations/payments/external"
        
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("x-integration-key", apiKey)

        val body = XGateCreatePaymentRequest(
            currency = "usdt",
            network = "trx", // Standard default for USDT
            amount = plan.priceUsd,
            orderId = orderId,
            callbacks = XGateCallbacks(
                success = successUrl,
                failed = failedUrl
            )
        )

        val request = HttpEntity(body, headers)
        
        return try {
            val response = restTemplate.postForObject(url, request, XGatePaymentResponse::class.java)
            val data = response?.data ?: throw RuntimeException("Empty response from 1xgate")
            
            CreatePaymentResponse(
                paymentLink = data.link,
                paymentId = data._id,
                orderId = data.orderId
            )
        } catch (e: Exception) {
            logger.error("Error creating payment at 1xgate: ${e.message}")
            throw RuntimeException("Failed to initiate payment gateway", e)
        }
    }

    fun getPaymentStatus(paymentId: String): XGatePaymentData? {
        val url = "$baseUrl/v1/integrations/payments/$paymentId"
        
        val headers = HttpHeaders()
        headers.set("x-integration-key", apiKey)
        
        val request = HttpEntity<Unit>(headers)
        
        return try {
            val response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, XGatePaymentResponse::class.java)
            response.body?.data
        } catch (e: Exception) {
            logger.error("Error getting payment status from 1xgate: ${e.message}")
            null
        }
    }
}
