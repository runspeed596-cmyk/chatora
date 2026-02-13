package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.api.dtos.*
import com.iliyadev.minichat.domain.entities.PaymentStatus
import com.iliyadev.minichat.domain.entities.PaymentTransaction
import com.iliyadev.minichat.domain.repositories.PaymentTransactionRepository
import com.iliyadev.minichat.domain.repositories.SubscriptionPlanRepository
import com.iliyadev.minichat.domain.repositories.UserRepository
import com.iliyadev.minichat.domain.services.SubscriptionService
import com.iliyadev.minichat.domain.services.XGatePaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/api")
class PaymentController(
    private val xgatePaymentService: XGatePaymentService,
    private val subscriptionService: SubscriptionService,
    private val userRepository: UserRepository,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) {

    @GetMapping("/subscription/plans")
    fun getPlans(): ResponseEntity<List<PlanDto>> {
        val plans = subscriptionPlanRepository.findAll().map { 
            PlanDto(it.name, it.months, it.priceUsd)
        }
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/subscription/status")
    fun getStatus(principal: Principal): ResponseEntity<SubscriptionStatusResponse> {
        val user = userRepository.findByUsername(principal.name).orElse(null)
            ?: return ResponseEntity.notFound().build()
        
        val activeSub = subscriptionService.getActiveSubscription(user.id!!)
        
        return ResponseEntity.ok(SubscriptionStatusResponse(
            isPremium = user.isPremium,
            premiumUntil = user.premiumUntil,
            plan = activeSub?.plan?.name
        ))
    }

    @PostMapping("/payment/create")
    fun createPayment(@RequestBody request: CreatePaymentRequest, principal: Principal): ResponseEntity<CreatePaymentResponse> {
        val user = userRepository.findByUsername(principal.name).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        val plan = subscriptionPlanRepository.findByName(request.plan)
            ?: return ResponseEntity.badRequest().build()
        
        val orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()
        
        if (plan.priceUsd == 0.0) {
            subscriptionService.activateSubscription(user.id!!, plan)
            return ResponseEntity.ok(CreatePaymentResponse(
                paymentLink = "SUCCESS",
                paymentId = "FREE-" + UUID.randomUUID().toString().take(8),
                orderId = orderId
            ))
        }

        val xgateResponse = xgatePaymentService.createPayment(user.id!!, plan, orderId)
        
        // Save transaction record
        val transaction = PaymentTransaction(
            user = user,
            externalPaymentId = xgateResponse.paymentId,
            orderId = orderId,
            status = PaymentStatus.CREATED,
            amount = plan.priceUsd,
            plan = plan
        )
        paymentTransactionRepository.save(transaction)
        
        return ResponseEntity.ok(xgateResponse)
    }

    @GetMapping("/payment/status/{paymentId}")
    fun checkPaymentStatus(@PathVariable paymentId: String): ResponseEntity<Map<String, Any>> {
        val data = xgatePaymentService.getPaymentStatus(paymentId)
            ?: return ResponseEntity.notFound().build()
            
        return ResponseEntity.ok(mapOf(
            "status" to data.status,
            "_id" to data._id,
            "orderId" to data.orderId
        ))
    }
}
