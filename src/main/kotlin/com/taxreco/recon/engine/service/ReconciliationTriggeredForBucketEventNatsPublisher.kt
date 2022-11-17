package com.taxreco.recon.engine.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.taxreco.recon.engine.model.ReconciliationTriggeredForBucketEvent
import com.taxreco.recon.engine.model.ReconciliationTriggeredForBucketEventPublisher
import io.nats.client.Connection
import io.nats.client.api.StreamConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.Charset

@Service
class ReconciliationTriggeredForBucketEventNatsPublisher(
    private val connection: Connection,
    @Value("\${reconStreamName}") private val reconStreamName: String,
    @Value("\${reconBucketTriggerSubject}") private val reconBucketTriggerSubject: String,
) : ReconciliationTriggeredForBucketEventPublisher {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    private val jacksonObjectMapper = jacksonObjectMapper()

    override fun publish(reconciliationTriggeredForBucketEvent: ReconciliationTriggeredForBucketEvent) {
        val topic = reconBucketTriggerSubject
        val json = jacksonObjectMapper.writeValueAsString(reconciliationTriggeredForBucketEvent)
        connection.jetStream().publish(
            topic,
            json.toByteArray(Charset.defaultCharset())
        )
    }

    override fun init(reconciliationTriggeredForBucketEvent: ReconciliationTriggeredForBucketEvent) {
        try {
            connection.jetStreamManagement().addStream(
                StreamConfiguration.Builder()
                    .name(reconStreamName)
                    .subjects(reconBucketTriggerSubject)
                    .build()
            )
        } catch (ex: Exception) {
            logger.error("Error while setting up the stream: ", ex)
            connection.jetStreamManagement().updateStream(
                StreamConfiguration.Builder()
                    .name(reconStreamName)
                    .subjects(reconBucketTriggerSubject)
                    .build()
            )
        }
    }
}