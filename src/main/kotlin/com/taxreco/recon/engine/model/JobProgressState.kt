package com.taxreco.recon.engine.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Sharded


@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "jobProgressState")
@Sharded(shardKey = ["jobId"])
data class JobProgressState(
    @Id
    var id: String,
    var jobId: String,
)