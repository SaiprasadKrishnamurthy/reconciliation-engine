package com.taxreco.recon.engine.model

import java.util.*

object MatchKeyContext {
    private val keys = mutableMapOf<String, String>()

    fun keyFor(name: String): String {
        return UUID.randomUUID().toString()
    }

    fun clearAll() {
        keys.clear()
    }

    fun clear(name: String) {
        keys.remove(name)
    }
}