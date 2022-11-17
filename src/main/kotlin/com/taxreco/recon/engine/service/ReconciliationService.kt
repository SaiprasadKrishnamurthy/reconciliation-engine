package com.taxreco.recon.engine.service

import com.taxreco.recon.engine.config.TenantContext
import com.taxreco.recon.engine.model.*
import com.taxreco.recon.engine.repository.*
import com.taxreco.recon.engine.service.Functions.MATCH_KEY_ATTRIBUTE
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@Service
class ReconciliationService(
    private val applicationContext: ApplicationContext,
    private val transactionRecordRepository: TransactionRecordRepository,
    private val publisher: Publisher,
    private val matchRecordRepository: MatchRecordRepository,
    private val jobStatsRepository: JobStatsRepository,
    private val jobProgressStateRepository: JobProgressStateRepository,
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
            val expectedBucketValuesCount = AtomicLong(0L)
            matchRecordRepository.deleteByJobId(reconciliationTriggeredEvent.jobId)
            jobProgressStateRepository.deleteByJobId(reconciliationTriggeredEvent.jobId)
            jobStatsRepository.deleteByJobId(reconciliationTriggeredEvent.jobId)
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
                    expectedBucketValuesCount.getAndIncrement()
                    reconciliationTriggeredForBucketEventPublisher.publish(reconciliationTriggeredForBucketEvent)
                }
            jobStatsRepository.save(
                JobStats(
                    jobId = reconciliationTriggeredEvent.jobId,
                    expectedBucketValuesCount = expectedBucketValuesCount.toLong()
                )
            )
        } finally {
            tenantContext.clear()
        }
    }

    @Async("reconThreadPoolExecutor")
    fun reconcile(reconciliationTriggeredEvent: ReconciliationTriggeredForBucketEvent) {
        try {
//            logger.info(" Received: $reconciliationTriggeredEvent")
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
                        matchRecordRepository.save(
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
                publisher.init(reconciliationContext)
                val bvs = matchRecordRepository.getDistinctBucketValues(reconciliationContext.jobId)
                bvs.forEach { b ->
                    val mks = matchRecordRepository.getDistinctMatchKeys(reconciliationContext.jobId, b)
                    mks.forEach { mk ->
                        val matchResult = matchRecordRepository.getMatchResults(reconciliationContext.jobId, b, mk)
                        publisher.publish(
                            reconciliationContext,
                            matchResult
                        )
                        //logger.info(" Results Streamed $matchResult")
                    }
                }
            }
            jobProgressStateRepository.save(
                JobProgressState(
                    jobId = reconciliationTriggeredEvent.jobId,
                    id = "${reconciliationTriggeredEvent.jobId}_${reconciliationTriggeredEvent.ruleSetName}_${reconciliationTriggeredEvent.bucketValue}"
                )
            )
        } finally {
            val count = jobProgressStateRepository.countByJobId(reconciliationTriggeredEvent.jobId)
            val updateCount = jobStatsRepository.updateCount(reconciliationTriggeredEvent.jobId, count)
            updateCount?.let {
                val reconciliationJobProgressEvent = ReconciliationJobProgressEvent(
                    jobId = reconciliationTriggeredEvent.jobId,
                    tenantId = reconciliationTriggeredEvent.tenantId,
                    expectedCount = updateCount.expectedBucketValuesCount,
                    processedCount = count,
                    startedAt = it.startedAt,
                    endedAt = if (count >= it.expectedBucketValuesCount) System.currentTimeMillis() else null,
                    finished = count >= it.expectedBucketValuesCount
                )
                if(reconciliationJobProgressEvent.finished) {
                    logger.info(" ******************************** ${reconciliationTriggeredEvent.jobId} Finished for Tenant ${reconciliationTriggeredEvent.tenantId}  ******************************* ")
                }
                publisher.publish(reconciliationJobProgressEvent)
            }
            tenantContext.clear()
        }
    }
}