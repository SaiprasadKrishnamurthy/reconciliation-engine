package com.taxreco.recon.engine.model

data class MatchResult(
    val jobId: String,
    val matchKey: String,
    val matches: Map<String, List<Map<String, Any>>>,
    var matchTags: List<String>,
    val bucketValue: String
)