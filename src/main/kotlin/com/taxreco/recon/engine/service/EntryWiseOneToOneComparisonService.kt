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
class EntryWiseOneToOneComparisonService : RulesetEvaluationService {

    override fun match(
        reconciliationContext: ReconciliationContext,
        ruleSet: MatchRuleSet
    ) {
        if (supportedRulesetType() == ruleSet.type) {
            val parser: ExpressionParser = SpelExpressionParser()
            ruleSet.rules.forEach { rule ->
                val tokens = rule.expression!!.split("[\\p{Punct}\\s]+".toRegex())
                val datasources = tokens
                    .filter { reconciliationContext.reconciliationSetting.dataSources.map { d -> d.id }.contains(it) }
                val recordsA = reconciliationContext.transactionRecords[datasources[0]]
                    ?.map {
                        if (!it.attrs.containsKey(MATCH_KEY_ATTRIBUTE)) {
                            it.attrs[MATCH_KEY_ATTRIBUTE] = "\$${UUID.randomUUID()}"
                        }
                        it
                    }?.filter { it.attrs[MATCH_KEY_ATTRIBUTE].toString().startsWith("$") }
                    ?: emptyList()
                val recordsB = reconciliationContext.transactionRecords[datasources[1]]
                    ?.map {
                        if (!it.attrs.containsKey(MATCH_KEY_ATTRIBUTE)) {
                            it.attrs[MATCH_KEY_ATTRIBUTE] = "\$${UUID.randomUUID()}"
                        }
                        it
                    }?.filter { it.attrs[MATCH_KEY_ATTRIBUTE].toString().startsWith("$") }
                    ?: emptyList()
                val (big, small) = if (recordsA.size > recordsB.size) recordsA to recordsB else recordsB to recordsA
                big.forEach ca@{ a ->
                    small.forEach { b ->
                        if (!b.matchedWithKeys.contains(a.name)) {
                            val simpleContext = standardEvaluationContext(a.name, a.attrs, b.name, b.attrs)
                            val expr = rule.expression
                            val result = parser.parseExpression(expr).getValue(simpleContext)
                            if (result == true) {
                                val matchKey = rule.id + "__" + UUID.randomUUID().toString()
                                a.matchedWithKeys.add(b.name)
                                b.matchedWithKeys.add(a.name)
                                b.attrs[MATCH_KEY_ATTRIBUTE] = matchKey
                                a.attrs[MATCH_KEY_ATTRIBUTE] = matchKey
                                return@ca
                            }
                        }
                    }
                }
                addMatchTags(rule, recordsA, recordsB)
            }
        }
    }

    override fun supportedRulesetType(): RulesetType {
        return RulesetType.EntryWiseOneToOneChecks
    }

    private fun standardEvaluationContext(
        keyA: String,
        attsA: Map<String, Any>,
        keyB: String,
        attsB: Map<String, Any>
    ): StandardEvaluationContext {
        val simpleContext = StandardEvaluationContext(mapOf(keyA to attsA, keyB to attsB))
        simpleContext.addPropertyAccessor(MapAccessor())
        simpleContext.registerFunction(
            "valueWithinTolerance",
            Functions::class.java.getDeclaredMethod(
                "valueWithinTolerance",
                Double::class.java,
                Double::class.java,
                Double::class.java
            )
        )
        simpleContext.registerFunction(
            "multimatch",
            Functions::class.java.getDeclaredMethod(
                "multimatch",
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