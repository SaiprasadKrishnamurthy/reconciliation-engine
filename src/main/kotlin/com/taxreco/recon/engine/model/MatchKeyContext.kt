package com.taxreco.recon.engine.model

import java.util.*

object MatchKeyContext {
    private val keys = mutableMapOf<String, String>()

    fun keyFor(name: String): String {
        return keys.compute(name) { _, value ->
            value ?: UUID.randomUUID().toString()
        }!!
    }

    fun clearAll() {
        keys.clear()
    }

    fun clear(name: String) {
        keys.remove(name)
    }
}