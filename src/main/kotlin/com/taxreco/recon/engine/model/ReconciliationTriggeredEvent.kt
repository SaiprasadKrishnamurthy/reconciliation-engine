package com.taxreco.recon.engine.model

data class ReconciliationTriggeredEvent(
    val tenantId: String,
    val jobId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val reconSettingName: String,
    val reconSettingVersion: Long,
    val streamResults: Boolean
)