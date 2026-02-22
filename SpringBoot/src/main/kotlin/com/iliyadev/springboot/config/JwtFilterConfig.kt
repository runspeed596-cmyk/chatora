package com.iliyadev.springboot.config

import com.iliyadev.springboot.config.filters.JwtRequestFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JwtFilterConfig {
    @Autowired
    private lateinit var jwtRequestFilter: JwtRequestFilter

    @Bean
    fun jwtFilterRegister(): FilterRegistrationBean<*>? {
        val filterRegistrationBean: FilterRegistrationBean<JwtRequestFilter> = FilterRegistrationBean<JwtRequestFilter>()
        filterRegistrationBean.filter = jwtRequestFilter
        filterRegistrationBean.addUrlPatterns("/api/*")
        filterRegistrationBean.setName("jwtFilter")
        filterRegistrationBean.order = 1
        return filterRegistrationBean
    }

}