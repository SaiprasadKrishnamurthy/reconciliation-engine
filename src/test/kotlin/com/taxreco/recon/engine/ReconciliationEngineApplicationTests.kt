package com.taxreco.recon.engine

import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
@DataMongoTest
object ReconciliationEngineApplicationTests {

    @Container
    var mongoDBContainer = MongoDBContainer("mongo:4.4.2")

    @JvmStatic
    @DynamicPropertySource
    fun setProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.data.mongodb.uri") { mongoDBContainer.replicaSetUrl }
    }

    @Test
    fun contextLoads() {
    }

}
