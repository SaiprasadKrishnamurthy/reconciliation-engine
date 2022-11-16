package com.taxreco.recon.engine.model

interface ReconciliationTriggeredForBucketEventPublisher {
    fun publish(reconciliationTriggeredForBucketEvent: ReconciliationTriggeredForBucketEvent)
    fun init(reconciliationTriggeredForBucketEvent: ReconciliationTriggeredForBucketEvent)
}