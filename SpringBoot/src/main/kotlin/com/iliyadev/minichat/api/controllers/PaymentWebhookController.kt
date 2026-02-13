package com.iliyadev.minichat.api.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.iliyadev.minichat.api.dtos.XGateWebhookPayload
import com.iliyadev.minichat.domain.entities.PaymentStatus
import com.iliyadev.minichat.domain.repositories.PaymentTransactionRepository
import com.iliyadev.minichat.domain.services.SubscriptionService
import com.iliyadev.minichat.domain.services.XGatePaymentService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payment/1xgate")
class PaymentWebhookController(
    private val xgatePaymentService: XGatePaymentService,
    private val subscriptionService: SubscriptionService,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(PaymentWebhookController::class.java)

    @PostMapping("/webhook")
    fun handleWebhook(@RequestBody payload: XGateWebhookPayload): ResponseEntity<String> {
        logger.info("Received 1xgate webhook: $payload")
        
        val transaction = paymentTransactionRepository.findByExternalPaymentId(payload._id)
            ?: return ResponseEntity.status(404).body("Transaction not found")

        // Security: Always verify with 1xgate API that this payment is legit
        val verifiedData = xgatePaymentService.getPaymentStatus(payload._id)
        if (verifiedData == null || verifiedData.status != "success") {
            logger.warn("Webhook verification failed for transaction ${payload._id}. Status from API: ${verifiedData?.status}")
            return ResponseEntity.ok("Verification pending or failed")
        }

        if (transaction.status != PaymentStatus.SUCCESS) {
            transaction.status = PaymentStatus.SUCCESS
            transaction.webhookPayload = objectMapper.writeValueAsString(payload)
            paymentTransactionRepository.save(transaction)
            
            subscriptionService.activateSubscription(transaction.user.id!!, transaction.plan)
            logger.info("Subscription activated for user ${transaction.user.username} via webhook")
        }

        return ResponseEntity.ok("OK")
    }
}
