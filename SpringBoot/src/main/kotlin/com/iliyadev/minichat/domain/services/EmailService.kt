package com.iliyadev.minichat.domain.services

import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    fun sendVerificationCode(to: String, code: String) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            helper.setFrom("amooozeshbebin@gmail.com")
            helper.setTo(to)
            helper.setSubject("MiniChat Verification Code")
            
            val htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #4CAF50; text-align: center;">Welcome to MiniChat!</h2>
                    <p>Hello,</p>
                    <p>Thank you for registering. Please use the following code to verify your email address:</p>
                    <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #333;">
                        $code
                    </div>
                    <p>This code will expire in 10 minutes.</p>
                    <p>If you didn't request this code, please ignore this email.</p>
                    <hr style="border: null; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="font-size: 12px; color: #888; text-align: center;">&copy; 2026 MiniChat Team. All rights reserved.</p>
                </div>
            """.trimIndent()
            
            helper.setText(htmlContent, true)
            
            mailSender.send(message)
            logger.info("Verification email sent to $to")
        } catch (e: Exception) {
            logger.error("Failed to send verification email to $to: ${e.message}")
            throw RuntimeException("Could not send verification email", e)
        }
    }
}
