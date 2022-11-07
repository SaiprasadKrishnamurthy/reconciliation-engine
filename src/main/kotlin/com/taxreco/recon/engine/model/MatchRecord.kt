package com.taxreco.recon.engine.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Sharded

@Document(collection = "matchRecord")
@Sharded(shardKey = ["jobId"])
data class MatchRecord(
    @Id
    val id: String,
    val originalRecordId: String,
    val jobId: String,
    var matchTags: MutableSet<String> = mutableSetOf(),
    val buckets: MutableMap<String, String> = LinkedHashMap(),
    val matchKey: String,
    val records: List<Map<String, Any>>,
    val datasource: String
)