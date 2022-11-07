package com.taxreco.recon.engine.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "transactionRecord")
data class TransactionRecord(
    @Id
    val id: String,
    val name: String,
    val attrs: MutableMap<String, Any> = LinkedHashMap(),
    var matchTags: MutableSet<String> = mutableSetOf()
) {
    @Transient
    var matchedWithKeys: MutableSet<String> = mutableSetOf()
}