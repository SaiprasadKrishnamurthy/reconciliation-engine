package com.taxreco.recon.engine.repository

import com.taxreco.recon.engine.model.JobStats
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository

@Repository
class JobStatsRepository(private val mongoTemplate: MongoTemplate) {

    fun save(jobStats: JobStats) {
        mongoTemplate.save(jobStats)
    }

    fun updateCount(jobId: String, count: Long): JobStats? {
        val query = Query()
        query.addCriteria(Criteria.where("jobId").isEqualTo(jobId))
        val update = Update()
        update.set("actualBucketValuesCount", count)
        return mongoTemplate.findAndModify(query, update, JobStats::class.java)
    }

    fun deleteByJobId(jobId: String) {
        val query = Query()
        query.addCriteria(Criteria.where("jobId").isEqualTo(jobId))
        mongoTemplate.remove(query, JobStats::class.java)
    }
}