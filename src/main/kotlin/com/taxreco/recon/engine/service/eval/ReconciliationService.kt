package com.taxreco.recon.engine.service.eval

import com.taxreco.recon.engine.model.ReconciliationContext
import com.taxreco.recon.engine.model.RulesetEvaluationService
import com.taxreco.recon.engine.model.TransactionRecord
import com.taxreco.recon.engine.service.eval.Functions.MATCH_KEY_ATTRIBUTE
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class ReconciliationService(private val applicationContext: ApplicationContext) {

    fun reconcile(reconciliationContext: ReconciliationContext) {

        // Inject Data into the context. TODO
        val sales = listOf(
            TransactionRecord(
                id = "1",
                name = "sales",
                attrs = mutableMapOf("transactionAmount" to 1000.0, "tdsOnSales" to 100.0, "invoiceNo" to "1")
            ),
            TransactionRecord(
                id = "2",
                name = "sales",
                attrs = mutableMapOf("transactionAmount" to 1100.0, "tdsOnSales" to 111.0, "invoiceNo" to "2")
            ),
            TransactionRecord(
                id = "3",
                name = "sales",
                attrs = mutableMapOf("transactionAmount" to 11100.0, "tdsOnSales" to 1118.0, "invoiceNo" to "3")
            )
        )

        val tdsLedger = listOf(
            TransactionRecord(id = "1", name = "tdsLedger", attrs = mutableMapOf("tds" to 50.0, "invoiceNo" to "1")),
            TransactionRecord(id = "1", name = "tdsLedger", attrs = mutableMapOf("tds" to 50.0, "invoiceNo" to "1")),
            TransactionRecord(id = "2", name = "tdsLedger", attrs = mutableMapOf("tds" to 110.0, "invoiceNo" to "2")),
            TransactionRecord(id = "3", name = "tdsLedger", attrs = mutableMapOf("tds" to 110.0, "invoiceNo" to "3"))
        )

        reconciliationContext.addRecords("sales", sales)
        reconciliationContext.addRecords("tdsLedger", tdsLedger)

        val servs = applicationContext.getBeansOfType(RulesetEvaluationService::class.java).values
        reconciliationContext.reconciliationSetting.rulesets.forEach { rs ->
            servs.filter { it.supportedRulesetType() == rs.type }
                .forEach {
                    it.match(reconciliationContext, rs)
                }
        }

        cleanupMatchKeys(reconciliationContext)

        println("\n\n-----------\n\n")
        reconciliationContext.transactionRecords.forEach { kv ->
            println(kv.key)
            println(" -------------- ")
            kv.value.forEach { println(it) }
            println("\n\n")
        }

    }

    private fun cleanupMatchKeys(reconciliationContext: ReconciliationContext) {
        reconciliationContext.transactionRecords.forEach { e ->
            e.value.forEach { r ->
                if (r.attrs.containsKey(MATCH_KEY_ATTRIBUTE) && r.attrs[MATCH_KEY_ATTRIBUTE].toString()
                        .contains("__")
                ) {
                    r.attrs[MATCH_KEY_ATTRIBUTE] =
                        r.attrs[MATCH_KEY_ATTRIBUTE].toString().substringAfter("__").replace("__", "")
                }
            }
        }
    }
}