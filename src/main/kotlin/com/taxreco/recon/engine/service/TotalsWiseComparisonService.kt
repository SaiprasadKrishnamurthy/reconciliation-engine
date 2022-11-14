package com.taxreco.recon.engine.service

import com.taxreco.recon.engine.model.MatchRuleSet
import com.taxreco.recon.engine.model.ReconciliationContext
import com.taxreco.recon.engine.model.RulesetEvaluationService
import com.taxreco.recon.engine.model.RulesetType
import com.taxreco.recon.engine.service.Functions.MATCH_KEY_ATTRIBUTE
import org.springframework.context.expression.MapAccessor
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class TotalsWiseComparisonService : RulesetEvaluationService {

    override fun match(
        reconciliationContext: ReconciliationContext,
        ruleSet: MatchRuleSet
    ) {
        if (supportedRulesetType() == ruleSet.type) {
            val parser: ExpressionParser = SpelExpressionParser()
            ruleSet.rules.forEach { rule ->
                val tokens = rule.expression!!.split("[\\p{Punct}\\s&&[^_]]+".toRegex())
                val datasources = tokens
                    .filter { reconciliationContext.reconciliationSetting.dataSources.map { d -> d.id }.contains(it) }
                    .distinct()
                val recordsA = reconciliationContext.transactionRecords[datasources[0]] ?: emptyList()
                val recordsB = reconciliationContext.transactionRecords[datasources[1]] ?: emptyList()
                val simpleContext = standardEvaluationContext(datasources[0],
                    recordsA.map {
                        if (!it.attrs.containsKey(MATCH_KEY_ATTRIBUTE)) {
                            it.attrs[MATCH_KEY_ATTRIBUTE] = "\$${UUID.randomUUID()}}"
                            it.attrs
                        } else {
                            it.attrs
                        }
                    },
                    datasources[1],
                    recordsB.map {
                        if (!it.attrs.containsKey(MATCH_KEY_ATTRIBUTE)) {
                            it.attrs[MATCH_KEY_ATTRIBUTE] = "\$${UUID.randomUUID()}}"
                            it.attrs
                        } else {
                            it.attrs
                        }
                    }
                )

                rule.expression.let {
                    parser.parseExpression(it).getValue(simpleContext)
                }
                addMatchTags(rule, recordsA, recordsB)
            }
        }
    }

    override fun supportedRulesetType(): RulesetType {
        return RulesetType.TotalsChecks
    }

    private fun standardEvaluationContext(
        keyA: String,
        attsA: List<Map<String, Any>>,
        keyB: String,
        attsB: List<Map<String, Any>>
    ): StandardEvaluationContext {
        val simpleContext = StandardEvaluationContext(mapOf(keyA to attsA, keyB to attsB))
        simpleContext.addPropertyAccessor(MapAccessor())
        simpleContext.registerFunction(
            "totalEqualsWithNumericTolerance",
            Functions::class.java.getDeclaredMethod(
                "totalEqualsWithNumericTolerance",
                String::class.java,
                List::class.java,
                List::class.java,
                List::class.java,
                List::class.java,
                String::class.java,
                String::class.java,
                Double::class.java
            )
        )
        return simpleContext
    }
}