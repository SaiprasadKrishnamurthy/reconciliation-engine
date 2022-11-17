package com.taxreco.recon.engine.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.taxreco.recon.engine.model.MatchResult
import com.taxreco.recon.engine.model.MatchResultPublisher
import com.taxreco.recon.engine.model.ReconciliationContext
import io.nats.client.Connection
import io.nats.client.api.StreamConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.Charset

@Service
class MatchResultsNatsPublisher(
    private val connection: Connection, @Value("\${reconStreamName}") private val reconStreamName: String,
    @Value("\${reconTriggerSubject}") private val reconTriggerSubject: String,
    @Value("\${reconBucketTriggerSubject}") private val reconBucketTriggerSubject: String
    ) :
    MatchResultPublisher {

    private val jacksonObjectMapper = jacksonObjectMapper()

    override fun publish(reconciliationContext: ReconciliationContext, matchResult: MatchResult) {
        val topic = reconciliationContext.tenantId + ".reconresult"
        val json = jacksonObjectMapper.writeValueAsString(matchResult)
        connection.jetStream().publish(
            topic,
            json.toByteArray(Charset.defaultCharset())
        )
    }

    override fun init(reconciliationContext: ReconciliationContext) {
        try {
            connection.jetStreamManagement().addStream(
                StreamConfiguration.Builder()
                    .name(reconStreamName)
                    .addSubjects(reconTriggerSubject, reconBucketTriggerSubject, reconciliationContext.tenantId + ".reconresult")
                    .build()
            )
        } catch (ex: Exception) {
            connection.jetStreamManagement().updateStream(
                StreamConfiguration.Builder()
                    .name(reconStreamName)
                    .addSubjects(reconTriggerSubject, reconBucketTriggerSubject, reconciliationContext.tenantId + ".reconresult")
                    .build()
            )
        }
    }
}