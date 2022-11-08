package com.taxreco.recon.engine.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Sharded

data class RecordId(val idField: String, val name: String)

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "transactionRecord")
@Sharded(shardKey = ["name"])
data class TransactionRecord(

    @JsonAlias("_id")
    @Id
    var id: RecordId? = null,
    val name: String,
    val attrs: MutableMap<String, Any> = LinkedHashMap(),
    @Transient
    var matchTags: MutableSet<String> = mutableSetOf()
) {
    @Transient
    var matchedWithKeys: MutableSet<String> = mutableSetOf()
}