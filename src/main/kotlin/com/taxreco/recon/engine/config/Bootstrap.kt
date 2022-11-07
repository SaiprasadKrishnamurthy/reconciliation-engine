package com.taxreco.recon.engine.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.vincentrussell.query.mongodb.sql.converter.QueryConverter
import com.taxreco.recon.engine.model.ReconciliationContext
import com.taxreco.recon.engine.model.ReconciliationSetting
import com.taxreco.recon.engine.model.TransactionRecord
import com.taxreco.recon.engine.service.eval.ReconciliationService
import org.bson.Document
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import java.io.FileInputStream
import java.util.*

@Configuration
class Bootstrap {

    @Bean
    fun start(
        mongoTemplate: MongoTemplate,
        tenantContext: TenantContext,
        reconciliationService: ReconciliationService
    ) = CommandLineRunner {
//        mongoSample(mongoTemplate)

        foo(reconciliationService)
    }

    private fun mongoSample(mongoTemplate: MongoTemplate) {
        val transactionRecord = TransactionRecord(
            id = "1",
            name = "one",
            hashMapOf("ProfessionalServices" to 1000.0, "TdsOnSales" to 100.0, "InvoiceNumber" to "INV1")
        )
        mongoTemplate.save(transactionRecord)

        val all = mongoTemplate.findAll(TransactionRecord::class.java)

        val queryConverter = QueryConverter.Builder()
            .sqlString("select * from transactionRecord where attrs.ProfessionalServices > 500")
            .build()

        val mongoDBQueryHolder = queryConverter.mongoQuery
        val collection = mongoTemplate.getCollection(mongoDBQueryHolder.collection)
        collection.find(mongoDBQueryHolder.query)
            .projection(mongoDBQueryHolder.projection)
            .map { a: Document -> a }
            .forEach { println(it.toJson()) }
    }

    private fun foo(reconciliationService: ReconciliationService) {
        val reconciliationSetting =
            jacksonObjectMapper().readValue(FileInputStream("ReconSetting.json"), ReconciliationSetting::class.java)
        val rc =
            ReconciliationContext(
                reconciliationSetting = reconciliationSetting,
                jobId = UUID.randomUUID().toString(),
                tenantId = "local"
            )
        reconciliationService.reconcile(rc)
    }
}