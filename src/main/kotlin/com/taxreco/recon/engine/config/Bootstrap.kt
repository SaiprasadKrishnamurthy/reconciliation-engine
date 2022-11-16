package com.taxreco.recon.engine.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.taxreco.recon.engine.model.ReconciliationSetting
import com.taxreco.recon.engine.model.ReconciliationTriggeredEvent
import com.taxreco.recon.engine.model.RecordId
import com.taxreco.recon.engine.model.TransactionRecord
import com.taxreco.recon.engine.repository.MatchRecordRepository
import com.taxreco.recon.engine.service.MatchResultsNatsPublisher
import com.taxreco.recon.engine.service.ReconciliationService
import io.nats.client.Connection
import org.springframework.beans.factory.annotation.Value
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
        @Value("\${reconTriggerSubject}") reconTriggerSubject: String,
        connection: Connection,
        mongoTemplate: MongoTemplate,
        tenantContext: TenantContext,
        reconciliationService: ReconciliationService,
        matchRecordRepository: MatchRecordRepository,
        matchResultsNatsPublisher: MatchResultsNatsPublisher
    ) = CommandLineRunner {
        tenantContext.tenantId = "local"
        mongoSample(mongoTemplate)

        repeat((0 until 1).count()) {
            val jobId = UUID.randomUUID().toString()
            connection.jetStream().publish(
                reconTriggerSubject,
                jacksonObjectMapper().writeValueAsBytes(
                    ReconciliationTriggeredEvent(
                        tenantId = "local",
                        reconSettingName = "Settings 1",
                        reconSettingVersion = 1,
                        streamResults = true,
                        jobId = jobId,

                    )
                )
            )
        }
    }

    private fun mongoSample(mongoTemplate: MongoTemplate) {
        mongoTemplate.dropCollection(TransactionRecord::class.java)
        mongoTemplate.dropCollection(ReconciliationSetting::class.java)
        val sales = listOf(
            TransactionRecord(
                id = RecordId("1", "sales"),
                name = "sales",
                attrs = mutableMapOf(
                    "transactionAmount" to 1000.0,
                    "tdsOnSales" to 100.0,
                    "invoiceNo" to "1",
                    "entityId" to "1"
                )
            ),
            TransactionRecord(
                id = RecordId("2", "sales"),
                name = "sales",
                attrs = mutableMapOf(
                    "transactionAmount" to 1100.0,
                    "tdsOnSales" to 111.0,
                    "invoiceNo" to "2",
                    "entityId" to "1"
                )
            ),
            TransactionRecord(
                id = RecordId("3", "sales"),
                name = "sales",
                attrs = mutableMapOf(
                    "transactionAmount" to 11100.0,
                    "tdsOnSales" to 1118.0,
                    "invoiceNo" to "3",
                    "entityId" to "1"
                )
            )
        )

        val tdsLedger = listOf(
            TransactionRecord(
                id = RecordId("1", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 50.0, "invoiceNo" to "1", "entityId" to "1")
            ),
            TransactionRecord(
                id = RecordId("2", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 50.0, "invoiceNo" to "1", "entityId" to "1")
            ),
            TransactionRecord(
                id = RecordId("3", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 110.0, "invoiceNo" to "2", "entityId" to "1")
            ),
            TransactionRecord(
                id = RecordId("4", "tdsLedger"),
                name = "tdsLedger",
                attrs = mutableMapOf("tds" to 320.0, "invoiceNo" to "3", "entityId" to "1")
            )
        )

        val t6as = listOf(
            TransactionRecord(
                id = RecordId("1", "_26as"),
                name = "_26as",
                attrs = mutableMapOf("amountPaid" to 1100.0, "taxFiled" to 110.0, "invoiceNo" to "2", "entityId" to "1")
            ),
            TransactionRecord(
                id = RecordId("2", "_26as"),
                name = "_26as",
                attrs = mutableMapOf("amountPaid" to 1600.0, "taxFiled" to 160.0, "entityId" to "1")
            )
        )

        sales.forEach { mongoTemplate.save(it) }
        tdsLedger.forEach { mongoTemplate.save(it) }
        t6as.forEach { mongoTemplate.save(it) }

        val files = arrayOf(
            "ReconSettings.json",
        )
        files.forEach { f ->
            val reconciliationSetting =
                jacksonObjectMapper().readValue(
                    FileInputStream(f),
                    ReconciliationSetting::class.java
                )
            mongoTemplate.save(reconciliationSetting)
        }
    }

    /*private fun foo(reconciliationService: ReconciliationService, matchRecordRepository: MatchRecordRepository) {
        val files = arrayOf(
            "ReconSettings.json",
        )
        files.forEach { f ->
            val reconciliationSetting =
                jacksonObjectMapper().readValue(
                    FileInputStream(f),
                    ReconciliationSetting::class.java
                )
            reconciliationSetting.rulesets.forEach { rs ->
                val rc =
                    ReconciliationContext(
                        reconciliationSetting = reconciliationSetting,
                        jobId = UUID.randomUUID().toString(),
                        tenantId = "local",
                        bucketValue = "Entity1",
                        ruleset = rs
                    )
                reconciliationService.reconcile(rc)
            }
        }
    }*/
}