package com.taxreco.recon.engine.model

data class ReconciliationTriggeredForBucketEvent(
    val jobId: String,
    val tenantId: String,
    val startedAt: Long,
    val reconSettingName: String,
    val reconSettingVersion: Long,
    val ruleSetName: String,
    val streamResults: Boolean,
    val bucketValue: String
)