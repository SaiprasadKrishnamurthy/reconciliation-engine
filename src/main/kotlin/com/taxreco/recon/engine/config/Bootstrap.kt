package com.taxreco.recon.engine.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.taxreco.recon.engine.model.ReconciliationContext
import com.taxreco.recon.engine.model.ReconciliationSetting
import com.taxreco.recon.engine.model.RecordId
import com.taxreco.recon.engine.model.TransactionRecord
import com.taxreco.recon.engine.repository.MatchRecordRepository
import com.taxreco.recon.engine.service.ReconciliationService
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
        reconciliationService: ReconciliationService,
        matchRecordRepository: MatchRecordRepository
    ) = CommandLineRunner {
        tenantContext.tenantId = "omlogistics"
        mongoSample(tenantContext, mongoTemplate)
        foo(reconciliationService, matchRecordRepository)
    }

    private fun mongoSample(tenantContext: TenantContext, mongoTemplate: MongoTemplate) {
        mongoTemplate.dropCollection(TransactionRecord::class.java)
        val sales = listOf(
            TransactionRecord(
                id = RecordId("1", "sales"),
                name = "sales",
                attrs = mutableMapOf("transactionAmount" to 1000.0, "tdsOnSales" to 100.0, "invoiceNo" to "1")
            ),
            TransactionRecord(
                id = RecordId("2", "sales"),
                name = "sales",
                attrs = mutableMapOf("transactionAmount" to 1100.0, "tdsOnSales" to 111.0, "invoiceNo" to "2")
            ),
            TransactionRecord(
                id = RecordId("3", "sales"),
                name = "sales",
                attrs = mutableMapOf("transactionAmount" to 11100.0, "tdsOnSales" to 1118.0, "invoiceNo" to "3")
            )
        )

        val tdsLedger = listOf(
            TransactionRecord(
                id = RecordId("1", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 50.0, "invoiceNo" to "1")
            ),
            TransactionRecord(
                id = RecordId("2", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 50.0, "invoiceNo" to "1")
            ),
            TransactionRecord(
                id = RecordId("3", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 110.0, "invoiceNo" to "2")
            ),
            TransactionRecord(
                id = RecordId("4", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 320.0, "invoiceNo" to "3")
            )
        )

        val t6as = listOf(
            TransactionRecord(
                id = RecordId("1", "_26as"),
                name = "_26as",
                attrs = mutableMapOf("amountPaid" to 1100.0, "taxFiled" to 110.0, "invoiceNo" to "2")
            ),
            TransactionRecord(
                id = RecordId("2", "_26as"),
                name = "_26as",
                attrs = mutableMapOf("amountPaid" to 1600.0, "taxFiled" to 160.0)
            )
        )

        sales.forEach { mongoTemplate.save(it) }
        tdsLedger.forEach { mongoTemplate.save(it) }
        t6as.forEach { mongoTemplate.save(it) }
    }

    private fun foo(reconciliationService: ReconciliationService, matchRecordRepository: MatchRecordRepository) {
        val files = arrayOf(
            "ReconSettingFieldChecks.json",
            "ReconSettingTotalsChecks.json",
            "ReconSettingTransactionChecks.json"
        )
        files.forEach { f ->
            val reconciliationSetting =
                jacksonObjectMapper().readValue(
                    FileInputStream(f),
                    ReconciliationSetting::class.java
                )
            val rc =
                ReconciliationContext(
                    reconciliationSetting = reconciliationSetting,
                    jobId = UUID.randomUUID().toString(),
                    tenantId = "local",
                    bucketValue = "Entity1"
                )
            reconciliationService.reconcile(rc)
        }
    }
}