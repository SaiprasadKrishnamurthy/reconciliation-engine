package com.taxreco.recon.engine.model

data class ReconciliationSetting(
    val dataSources: List<DataSource>,
    val id: String,
    val name: String,
    val group: String,
    val rulesets: List<MatchRuleSet>
)