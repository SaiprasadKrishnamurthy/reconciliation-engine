package com.taxreco.recon.engine.repository

import com.taxreco.recon.engine.model.DataSet
import com.taxreco.recon.engine.model.MatchRecord
import com.taxreco.recon.engine.model.MatchResult
import com.taxreco.recon.engine.service.Functions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository

@Repository
class MatchRecordRepository(private val mongoTemplate: MongoTemplate) {

    fun getDistinctBucketValues(jobId: String): List<String> {
        val query = Query()
        query.addCriteria(Criteria.where("jobId").isEqualTo(jobId))
        return mongoTemplate.findDistinct(query, "bucketValue", MatchRecord::class.java, String::class.java)
    }

    fun getDistinctMatchKeys(jobId: String, bucketValue: String): List<String> {
        val query = Query()
        query.addCriteria(Criteria.where("jobId").isEqualTo(jobId).and("bucketValue").isEqualTo(bucketValue))
        return mongoTemplate.findDistinct(query, "matchKey", MatchRecord::class.java, String::class.java)
    }

    fun getMatchResults(jobId: String, bucketValue: String, matchKey: String): MatchResult {
        val query = Query()
        query.addCriteria(
            Criteria.where("jobId").isEqualTo(jobId)
                .and("bucketValue").isEqualTo(bucketValue)
                .and("matchKey").isEqualTo(matchKey)
        )
        val records = mongoTemplate.find(query, MatchRecord::class.java)
        val matches = records.groupBy { it.datasource }
            .mapValues { it.value.map { v -> v.record.filter { a -> a.key != Functions.MATCH_KEY_ATTRIBUTE } } }
        val matchTags = records.flatMap { it.tags }.distinct()
        return MatchResult(
            jobId = jobId,
            matchKey = matchKey,
            groupName = if (records.isNotEmpty()) records[0].groupName else "",
            dataset = DataSet(matches, matchTags),
            bucketValue = bucketValue,
            rulesetType = if (records.isNotEmpty()) records[0].rulesetType else null,
        )
    }
}