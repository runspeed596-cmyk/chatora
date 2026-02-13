package com.iliyadev.minichat.core.seeding

import com.iliyadev.minichat.domain.entities.Role
import com.iliyadev.minichat.domain.entities.User
import com.iliyadev.minichat.domain.repositories.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AdminSeeder(
    private val userRepository: UserRepository,
    private val subscriptionPlanRepository: com.iliyadev.minichat.domain.repositories.SubscriptionPlanRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        seedAdmin()
        seedPlans()
    }

    private fun seedAdmin() {
        val existingAdmin = userRepository.findByUsername("admin")
        if (existingAdmin.isPresent) {
            val admin = existingAdmin.get()
            admin.password = passwordEncoder.encode("admin")
            admin.role = Role.ADMIN
            userRepository.save(admin)
            println(">>>> PRODUCTION SECURITY: Admin password and role verified/updated.")
        } else {
            val admin = User(
                username = "admin",
                deviceId = "admin_hardware_id",
                password = passwordEncoder.encode("admin"),
                role = Role.ADMIN
            )
            userRepository.save(admin)
            println(">>>> PRODUCTION SECURITY: New Admin seeded.")
        }
    }

    private fun seedPlans() {
        if (subscriptionPlanRepository.count() == 0L) {
            val plans = listOf(
                com.iliyadev.minichat.domain.entities.SubscriptionPlan(name = "یک ماهه", months = 1, priceUsd = 2.99),
                com.iliyadev.minichat.domain.entities.SubscriptionPlan(name = "سه ماهه", months = 3, priceUsd = 7.99),
                com.iliyadev.minichat.domain.entities.SubscriptionPlan(name = "یک ساله", months = 12, priceUsd = 17.99)
            )
            subscriptionPlanRepository.saveAll(plans)
            println(">>>> PRODUCTION: Default subscription plans seeded.")
        }
    }
}
