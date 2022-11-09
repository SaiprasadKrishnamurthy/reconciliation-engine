package com.taxreco.recon.engine.service

import com.taxreco.recon.engine.model.MatchRuleSet
import com.taxreco.recon.engine.model.ReconciliationContext
import com.taxreco.recon.engine.model.RulesetEvaluationService
import com.taxreco.recon.engine.model.RulesetType
import org.springframework.context.expression.MapAccessor
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class FieldChecksService : RulesetEvaluationService {

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

                reconciliationContext.transactionRecords[datasources[0]]?.forEach { rec ->
                    try {
                        val simpleContext = standardEvaluationContext(rec.name, rec.attrs)
                        val expr = rule.expression
                        val result = parser.parseExpression(expr).getValue(simpleContext)
                        if (result == true) {
                            rec.matchTags.addAll(rule.tagsWhenMatched)
                            rec.attrs[Functions.MATCH_KEY_ATTRIBUTE] = UUID.randomUUID().toString()
                        } else {
                            rec.matchTags.addAll(rule.tagsWhenNotMatched)
                        }
                    } catch (ex: Exception) {
                        rec.matchTags.addAll(rule.tagsWhenNotMatched)
                    }
                }
            }
        }
    }

    override fun supportedRulesetType(): RulesetType {
        return RulesetType.FieldChecks
    }

    private fun standardEvaluationContext(
        keyA: String,
        attsA: Map<String, Any>
    ): StandardEvaluationContext {
        val simpleContext = StandardEvaluationContext(mapOf(keyA to attsA))
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
        return simpleContext
    }
}