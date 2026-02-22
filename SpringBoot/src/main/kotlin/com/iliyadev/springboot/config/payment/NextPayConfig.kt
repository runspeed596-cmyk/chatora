package com.iliyadev.springboot.config.payment

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("payment.nextpay")
class NextPayConfig {

    var apikey: String = ""

    var tokenEndPoint: String = ""

    var callbackUri: String = ""

    var paymentUri: String = ""

    var verifyUri: String = ""
}