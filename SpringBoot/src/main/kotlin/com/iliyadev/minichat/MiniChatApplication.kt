package com.iliyadev.minichat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class MiniChatApplication

fun main(args: Array<String>) {
    runApplication<MiniChatApplication>(*args)
}
