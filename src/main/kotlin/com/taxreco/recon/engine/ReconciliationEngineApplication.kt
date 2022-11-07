package com.taxreco.recon.engine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReconciliationEngineApplication

fun main(args: Array<String>) {
    runApplication<ReconciliationEngineApplication>(*args)
}
