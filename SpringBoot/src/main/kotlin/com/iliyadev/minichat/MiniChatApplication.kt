package com.iliyadev.minichat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class ChatoraApplication

fun main(args: Array<String>) {
    runApplication<ChatoraApplication>(*args)
}
