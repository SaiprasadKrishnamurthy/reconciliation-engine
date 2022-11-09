package com.taxreco.recon.engine.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("tags", "records")
class DataSet(val records: Map<String, List<Map<String, Any>>>, val tags: List<String>)

@JsonPropertyOrder("bucketValue", "groupName", "rulesetType", "mismatch", "dataset", "matchKey", "jobId")
data class MatchResult(
    val jobId: String,
    val matchKey: String,
    val groupName: String,
    @JsonProperty("matchMetadata")
    val dataset: DataSet,
    val bucketValue: String,
    val rulesetType: RulesetType?
) {
    val mismatch: Boolean
        get() = matchKey.startsWith("$")
}