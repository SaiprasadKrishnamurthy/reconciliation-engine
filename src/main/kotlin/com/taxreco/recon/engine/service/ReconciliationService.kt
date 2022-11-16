package com.taxreco.recon.engine.service

import com.taxreco.recon.engine.config.TenantContext
import com.taxreco.recon.engine.model.*
import com.taxreco.recon.engine.repository.MatchRecordRepository
import com.taxreco.recon.engine.repository.ReconciliationSettingRepository
import com.taxreco.recon.engine.repository.TransactionRecordRepository
import com.taxreco.recon.engine.service.Functions.MATCH_KEY_ATTRIBUTE
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

@Service
class ReconciliationService(
    private val applicationContext: ApplicationContext,
    private val transactionRecordRepository: TransactionRecordRepository,
    private val mongoTemplate: MongoTemplate,
    private val matchResultPublisher: MatchResultPublisher,
    private val matchRecordRepository: MatchRecordRepository,
    private val reconciliationSettingRepository: ReconciliationSettingRepository,
    private val tenantContext: TenantContext,
    private val reconciliationTriggeredForBucketEventPublisher: ReconciliationTriggeredForBucketEventPublisher
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @Async("reconThreadPoolExecutor")
    fun reconcile(reconciliationTriggeredEvent: ReconciliationTriggeredEvent) {
        try {
            tenantContext.tenantId = reconciliationTriggeredEvent.tenantId
            val setting = reconciliationSettingRepository.findByNameAndVersion(
                reconciliationTriggeredEvent.reconSettingName,
                reconciliationTriggeredEvent.reconSettingVersion
            )

            // partition the data.
            setting.dataSources.flatMap { ds ->
                transactionRecordRepository.findBuckets(ds)
            }.distinct()
                .flatMap { bucket ->
                    setting.rulesets.map { rs ->
                        ReconciliationTriggeredForBucketEvent(
                            tenantId = reconciliationTriggeredEvent.tenantId,
                            reconSettingName = reconciliationTriggeredEvent.reconSettingName,
                            reconSettingVersion = reconciliationTriggeredEvent.reconSettingVersion,
                            streamResults = reconciliationTriggeredEvent.streamResults,
                            bucketValue = bucket,
                            ruleSetName = rs.name,
                            startedAt = reconciliationTriggeredEvent.startedAt,
                            jobId = reconciliationTriggeredEvent.jobId
                        )
                    }
                }.forEachIndexed { index, reconciliationTriggeredForBucketEvent ->
                    // set up only for the first time.
                    if (index == 0) {
                        reconciliationTriggeredForBucketEventPublisher.init(reconciliationTriggeredForBucketEvent)
                    }
                    reconciliationTriggeredForBucketEventPublisher.publish(reconciliationTriggeredForBucketEvent)
                }
        } finally {
            tenantContext.clear()
        }
    }

    @Async("reconThreadPoolExecutor")
    fun reconcile(reconciliationTriggeredEvent: ReconciliationTriggeredForBucketEvent) {
        try {
            logger.info(" Received: $reconciliationTriggeredEvent")
            tenantContext.tenantId = reconciliationTriggeredEvent.tenantId
            val reconciliationSetting = reconciliationSettingRepository.findByNameAndVersion(
                reconciliationTriggeredEvent.reconSettingName,
                reconciliationTriggeredEvent.reconSettingVersion
            )

            // initialize the context object
            val reconciliationContext = ReconciliationContext(
                jobId = reconciliationTriggeredEvent.jobId,
                tenantId = reconciliationTriggeredEvent.tenantId,
                bucketValue = reconciliationTriggeredEvent.bucketValue,
                startedAt = reconciliationTriggeredEvent.startedAt,
                ruleset = reconciliationSetting.rulesets.first { it.name == reconciliationTriggeredEvent.ruleSetName },
                streamResults = reconciliationTriggeredEvent.streamResults,
                reconciliationSetting = reconciliationSetting
            )

            reconciliationSetting.dataSources.forEach { ds ->
                val transactionRecords =
                    transactionRecordRepository.getTransactionRecords(ds, reconciliationTriggeredEvent.bucketValue)
                reconciliationContext.addRecords(ds.id, transactionRecords)
            }

            val servs = applicationContext.getBeansOfType(RulesetEvaluationService::class.java).values
            servs.filter { it.supportedRulesetType() == reconciliationContext.ruleset.type }
                .forEach {
                    it.match(reconciliationContext, reconciliationContext.ruleset)
                }

            reconciliationContext.transactionRecords
                .forEach { kv ->
                    kv.value.filter { it.matchTags.isNotEmpty() }.forEach { tr ->
                        mongoTemplate.save(
                            MatchRecord(
                                id = UUID.randomUUID().toString(),
                                originalRecordId = tr.id!!.idField,
                                groupName = reconciliationContext.reconciliationSetting.group,
                                jobId = reconciliationContext.jobId,
                                bucketKey = reconciliationContext.reconciliationSetting.dataSources.first { it.id == tr.name }.bucketField,
                                bucketValue = reconciliationContext.bucketValue,
                                matchKey = tr.attrs[MATCH_KEY_ATTRIBUTE]?.toString() ?: "",
                                record = tr.attrs,
                                tags = tr.matchTags,
                                datasource = tr.name,
                                rulesetType = reconciliationContext.ruleset.type
                            )
                        )
                    }
                }

            if (reconciliationContext.streamResults) {
                matchResultPublisher.init(reconciliationContext)
                val bvs = matchRecordRepository.getDistinctBucketValues(reconciliationContext.jobId)
                bvs.forEach { b ->
                    val mks = matchRecordRepository.getDistinctMatchKeys(reconciliationContext.jobId, b)
                    mks.forEach { mk ->
                        matchResultPublisher.publish(
                            reconciliationContext,
                            matchRecordRepository.getMatchResults(reconciliationContext.jobId, b, mk)
                        )
                    }
                }
            }
        } finally {
            tenantContext.clear()
        }
    }
}