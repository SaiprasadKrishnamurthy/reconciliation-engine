package com.taxreco.recon.engine.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.taxreco.recon.engine.model.MatchResult
import com.taxreco.recon.engine.model.MatchResultPublisher
import com.taxreco.recon.engine.model.ReconciliationContext
import io.nats.client.Connection
import io.nats.client.api.StreamConfiguration
import org.springframework.stereotype.Service
import java.nio.charset.Charset

@Service
class NatsPublisher(private val connection: Connection) : MatchResultPublisher {

    private val jacksonObjectMapper = jacksonObjectMapper()

    override fun publish(reconciliationContext: ReconciliationContext, matchResult: MatchResult) {
        val topic = reconciliationContext.tenantId + ".reconresult." + reconciliationContext.jobId
        connection.jetStream().publish(
            topic,
            jacksonObjectMapper.writeValueAsString(matchResult).toByteArray(Charset.defaultCharset())
        )
    }

    override fun init(reconciliationContext: ReconciliationContext) {
        try {
            connection.jetStreamManagement().addStream(
                StreamConfiguration.Builder()
                    .name(reconciliationContext.tenantId + "-streams")
                    .subjects(reconciliationContext.tenantId + ".reconresult." + reconciliationContext.jobId)
                    .build()
            )
        } catch (ex: Exception) {
            connection.jetStreamManagement().updateStream(
                StreamConfiguration.Builder()
                    .name(reconciliationContext.tenantId + "-streams")
                    .subjects(reconciliationContext.tenantId + ".reconresult." + reconciliationContext.jobId)
                    .build()
            )
        }
    }
}