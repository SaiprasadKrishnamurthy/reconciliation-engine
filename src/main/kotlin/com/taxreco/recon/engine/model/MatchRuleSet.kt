package com.taxreco.recon.engine.model

data class MatchRuleSet(
    val name: String,
    val type: RulesetType,
    val terminateWhenTagsPresent: List<String> = emptyList(),
    val rules: List<Rule>
)