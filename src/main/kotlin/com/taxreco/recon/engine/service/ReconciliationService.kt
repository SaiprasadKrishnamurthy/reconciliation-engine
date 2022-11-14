package com.taxreco.recon.engine.service

import com.taxreco.recon.engine.model.MatchRecord
import com.taxreco.recon.engine.model.MatchResultPublisher
import com.taxreco.recon.engine.model.ReconciliationContext
import com.taxreco.recon.engine.model.RulesetEvaluationService
import com.taxreco.recon.engine.repository.MatchRecordRepository
import com.taxreco.recon.engine.repository.TransactionRecordRepository
import com.taxreco.recon.engine.service.Functions.MATCH_KEY_ATTRIBUTE
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

@Service
class ReconciliationService(
    private val applicationContext: ApplicationContext,
    private val transactionRecordRepository: TransactionRecordRepository,
    private val mongoTemplate: MongoTemplate,
    @Value("\${stream.results}") private val streamResults: Boolean,
    private val matchResultPublisher: MatchResultPublisher,
    private val matchRecordRepository: MatchRecordRepository
) {

    fun reconcile(reconciliationContext: ReconciliationContext) {

        reconciliationContext.reconciliationSetting.dataSources.forEach { ds ->
            val transactionRecords =
                transactionRecordRepository.getTransactionRecords(ds, reconciliationContext.bucketValue)
            reconciliationContext.addRecords(ds.id, transactionRecords)
        }

        val servs = applicationContext.getBeansOfType(RulesetEvaluationService::class.java).values
        servs.filter { it.supportedRulesetType() == reconciliationContext.ruleset.type }
            .forEach {
                it.match(reconciliationContext, reconciliationContext.ruleset)
            }

        println("\n\n-----------\n\n")
        reconciliationContext.transactionRecords
            .forEach { kv ->
                kv.value.filter { it.matchTags.isNotEmpty() }.forEach { tr ->
                    mongoTemplate.save(
                        MatchRecord(
                            id = tr.id!!.idField + "_" + tr.id!!.name,
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

        if (streamResults) {
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

    }
}