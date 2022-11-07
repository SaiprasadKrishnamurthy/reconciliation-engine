package com.taxreco.recon.engine.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Rule(
    val expression: String? = null,
    val id: String,
    val tagsWhenMatched: List<String>,
    val tagsWhenNotMatched: List<String>,
)