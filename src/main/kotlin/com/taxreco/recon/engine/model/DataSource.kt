package com.taxreco.recon.engine.model

data class DataSource(
    val bucketFields: List<String>,
    val id: String,
    val predicate: String
)