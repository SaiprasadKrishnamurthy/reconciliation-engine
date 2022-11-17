package com.taxreco.recon.engine.model

interface Publisher {
    fun publish(reconciliationContext: ReconciliationContext, matchResult: MatchResult)
    fun init(reconciliationContext: ReconciliationContext)
    fun publish(progressEvent: ReconciliationJobProgressEvent)
}