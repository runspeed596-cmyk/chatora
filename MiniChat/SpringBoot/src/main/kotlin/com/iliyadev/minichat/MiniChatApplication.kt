package com.iliyadev.minichat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@EnableScheduling
@SpringBootApplication
class ChatoraApplication

fun main(args: Array<String>) {
    runApplication<ChatoraApplication>(*args)
}
