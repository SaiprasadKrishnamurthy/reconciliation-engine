package com.taxreco.recon.engine.service.eval

import com.taxreco.recon.engine.model.MatchKeyContext
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


internal class FunctionsTest {

    @BeforeEach
    internal fun setUp() {
        MatchKeyContext.clearAll()
    }

    @DisplayName("should match one to one with a single group by field for a and b")
    @Test
    fun multimatch() {
        val a: List<MutableMap<String, Any>> = listOf(
            mutableMapOf("id" to "A1", "tdsOnSales" to 100.0, "invoiceNo" to "1"),
            mutableMapOf("id" to "A2", "tdsOnSales" to 100.0, "invoiceNo" to "2"),
            mutableMapOf("id" to "A3", "tdsOnSales" to 110.0, "invoiceNo" to "3")
        )

        val b: List<MutableMap<String, Any>> = listOf(
            mutableMapOf("id" to "B1", "tds" to 100.0, "invoiceNo" to "1"),
            mutableMapOf("id" to "B2", "tds" to 100.0, "invoiceNo" to "2"),
            mutableMapOf("id" to "B3", "tds" to 110.0, "invoiceNo" to "3")
        )

        Functions.multimatch(
            "tds_at_transactions_level_between_sales_and_tds_ledger",
            a,
            b,
            listOf("invoiceNo"),
            listOf("invoiceNo"),
            "tdsOnSales",
            "tds",
            1.0
        )

        assertThat(a[0]["__matchkey"], equalTo(b[0]["__matchkey"]))

        assertThat(a[1]["__matchkey"], equalTo(b[1]["__matchkey"]))

        assertThat(a[2]["__matchkey"], equalTo(b[2]["__matchkey"]))
    }

    @DisplayName("should match one to many with a single group by field for a and b")
    @Test
    fun multimatch_many() {
        val a: List<MutableMap<String, Any>> = listOf(
            mutableMapOf("id" to "A1", "tdsOnSales" to 200.0, "invoiceNo" to "1")
        )

        val b: List<MutableMap<String, Any>> = listOf(
            mutableMapOf("id" to "B1", "tds" to 100.0, "invoiceNo" to "1"),
            mutableMapOf("id" to "B2", "tds" to 100.0, "invoiceNo" to "1"),
            mutableMapOf("id" to "B3", "tds" to 120.0, "invoiceNo" to "2"),
        )

        Functions.multimatch(
            "tds_at_transactions_level_between_sales_and_tds_ledger",
            a,
            b,
            listOf("invoiceNo"),
            listOf("invoiceNo"),
            "tdsOnSales",
            "tds",
            1.0
        )

        assertThat(a[0]["__matchkey"], equalTo(b[0]["__matchkey"]))
        assertThat(a[0]["__matchkey"], equalTo(b[1]["__matchkey"]))
        assertThat(b[2]["__matchkey"], `is`(nullValue()))
    }
}