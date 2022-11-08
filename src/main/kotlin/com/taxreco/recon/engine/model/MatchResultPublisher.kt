package com.taxreco.recon.engine.model

interface MatchResultPublisher {
    fun publish(reconciliationContext: ReconciliationContext, matchResult: MatchResult)
    fun init(reconciliationContext: ReconciliationContext)
}