package com.taxreco.recon.engine.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("reconciliationSetting")
data class ReconciliationSetting(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val version: Long = System.currentTimeMillis(),
    val created: Long = System.currentTimeMillis(),
    val owner: String = "",
    val dataSources: List<DataSource>,
    val group: String,
    val rulesets: List<MatchRuleSet>
)