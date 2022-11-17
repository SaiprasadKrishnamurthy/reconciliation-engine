package com.taxreco.recon.engine.model

data class ReconciliationJobProgressEvent(
    val tenantId: String,
    val jobId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val expectedCount: Long,
    val processedCount: Long,
    val finished: Boolean
)