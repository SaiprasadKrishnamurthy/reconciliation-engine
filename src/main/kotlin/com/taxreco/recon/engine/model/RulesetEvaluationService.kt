package com.taxreco.recon.engine.model

import com.taxreco.recon.engine.service.Functions.MATCH_KEY_ATTRIBUTE

interface RulesetEvaluationService {
    fun match(reconciliationContext: ReconciliationContext, ruleSet: MatchRuleSet)
    fun supportedRulesetType(): RulesetType

    fun addMatchTags(rule: Rule, recordsA: List<TransactionRecord>, recordsB: List<TransactionRecord>) {
        listOf(recordsB, recordsA)
            .flatten()
            .forEach { r ->
                if (!r.attrs.containsKey(MATCH_KEY_ATTRIBUTE) ||
                    r.attrs[MATCH_KEY_ATTRIBUTE]?.toString()?.startsWith("$") == true
                ) {
                    r.matchTags.addAll(rule.tagsWhenNotMatched)
                } else {
                    if (r.attrs[MATCH_KEY_ATTRIBUTE].toString().startsWith(rule.id)) {
                        r.matchTags.addAll(rule.tagsWhenMatched)
                    }
                }
            }
    }
}