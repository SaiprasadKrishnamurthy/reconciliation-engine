package com.taxreco.recon.engine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@EnableScheduling
@SpringBootApplication
class ReconciliationEngineApplication

fun main(args: Array<String>) {
    runApplication<ReconciliationEngineApplication>(*args)
}
