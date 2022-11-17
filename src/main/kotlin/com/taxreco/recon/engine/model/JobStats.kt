package com.taxreco.recon.engine.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Sharded


@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "jobStats")
@Sharded(shardKey = ["jobId"])
data class JobStats(

    @Id
    var jobId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val expectedBucketValuesCount: Long = 0L,
    val actualBucketValuesCount: Long = 0L,
)