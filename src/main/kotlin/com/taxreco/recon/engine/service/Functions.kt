package com.taxreco.recon.engine.service

import com.taxreco.recon.engine.model.MatchKeyContext
import kotlin.math.abs

object Functions {
    const val MATCH_KEY_ATTRIBUTE = "__matchkey"

    @JvmStatic
    fun valueWithinTolerance(a: Double, b: Double, t: Double): Boolean {
        return abs(a - b) <= t
    }

    @JvmStatic
    fun multimatch(
        name: String,
        a: List<MutableMap<String, Any>>,
        b: List<MutableMap<String, Any>>,
        groupByFieldsA: List<String>,
        groupByFieldsB: List<String>,
        oneFieldA: String,
        manyFieldB: String,
        tolerance: Double
    ) {
        try {
            val groupA = a.groupBy { d -> groupByFieldsA.map { d[it] ?: "" }.joinToString("_") }
            val groupB = b.groupBy { d -> groupByFieldsB.map { d[it] ?: "" }.joinToString("_") }

            groupA.forEach { ga ->
                val gb = groupB[ga.key]
                if (gb != null) {
                    ga.value.forEach { one ->
                        val valueA = one[oneFieldA]?.toString()?.toDoubleOrNull()
                        if (valueA != null) {
                            val arr = gb
                                .filter {
                                    it[MATCH_KEY_ATTRIBUTE] == null ||
                                            it[MATCH_KEY_ATTRIBUTE]?.toString()?.startsWith("\$") == true
                                }
                                .mapNotNull { it[manyFieldB]?.toString()?.toDoubleOrNull() }
                            val result = findElements(arr, valueA, tolerance)
                            if (result.isNotEmpty()) {
                                val key = MatchKeyContext.keyFor(name)
                                one[MATCH_KEY_ATTRIBUTE] = key
                                result.map { gb[it][MATCH_KEY_ATTRIBUTE] = key }
                            }
                        }
                    }
                }
            }
        } finally {
            MatchKeyContext.clear(name)
        }
    }

    @JvmStatic
    fun totalEqualsWithNumericTolerance(
        name: String,
        a: List<MutableMap<String, Any>>,
        b: List<MutableMap<String, Any>>,
        groupByFieldsA: List<String>,
        groupByFieldsB: List<String>,
        fieldA: String,
        fieldB: String,
        tolerance: Double
    ) {
        try {
            val groupA = a.groupBy { d -> groupByFieldsA.map { d[it] ?: "" }.joinToString("_") }
            val groupB = b.groupBy { d -> groupByFieldsB.map { d[it] ?: "" }.joinToString("_") }
            groupA.forEach { recA ->
                val sumA = recA.value.mapNotNull { it[fieldA]?.toString()?.toDoubleOrNull() }.sum()
                val sumB = groupB[recA.key]?.mapNotNull { it[fieldB]?.toString()?.toDoubleOrNull() }?.sum()
                if (sumB != null && abs(sumA - sumB) <= tolerance) {
                    val matchKey =MatchKeyContext.keyFor(name)
                    recA.value.forEach { ra ->
                        if (!ra.containsKey(MATCH_KEY_ATTRIBUTE) || ra[MATCH_KEY_ATTRIBUTE].toString()
                                .contains("$")
                        ) {
                            ra[MATCH_KEY_ATTRIBUTE] = matchKey
                        }
                    }
                    groupB[recA.key]?.forEach { rb ->
                        if (!rb.containsKey(MATCH_KEY_ATTRIBUTE) || rb[MATCH_KEY_ATTRIBUTE].toString()
                                .contains("$")
                        ) {
                            val correlatedMatchKey =
                                if (recA.value.isNotEmpty()) recA.value[0][MATCH_KEY_ATTRIBUTE].toString() else matchKey
                            rb[MATCH_KEY_ATTRIBUTE] = correlatedMatchKey
                        }
                    }
                }
            }
        } finally {
            MatchKeyContext.clear(name)
        }
    }

    private fun findElements(arr: List<Double>, n: Double, tolerance: Double = 0.0): List<Int> {
        val gathered = mutableListOf<Int>()
        parts(n, arr, gathered, 0.0, n, tolerance)
        return gathered
    }

    private fun parts(
        n: Double,
        arr: List<Double>,
        gathered: MutableList<Int>,
        acc: Double,
        target: Double = n,
        tolerance: Double
    ) {
        if (n <= 0) {
            if (acc != target && abs(acc - target) > tolerance) {
                gathered.clear()
            }
            return
        }
        val closest = when (val i = arr.indexOfFirst { it >= n }) {
            0 -> 0 to arr[0]
            arr.size - 1 -> arr.size - 1 to arr[arr.size - 1]
            -1 -> arr.size - 1 to arr[arr.size - 1]
            else -> i - 1 to arr[i - 1]
        }

        gathered.add(closest.first)
        parts(n - closest.second, arr, gathered, acc + closest.second, target, tolerance)
    }
}