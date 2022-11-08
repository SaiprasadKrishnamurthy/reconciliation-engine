package com.taxreco.recon.engine.model

data class ReconciliationContext(val jobId: String,
                                 val tenantId: String,
                                 val startedAt: Long = System.currentTimeMillis(),
                                 val bucketValue: String,
                                 val reconciliationSetting: ReconciliationSetting) {
    val transactionRecords: MutableMap<String, List<TransactionRecord>> = mutableMapOf()

    fun addRecords(name: String, records: List<TransactionRecord>) {
        transactionRecords[name] = records
    }

    fun getRecords(name: String): List<TransactionRecord> {
        return transactionRecords[name] ?: emptyList()
    }
}
