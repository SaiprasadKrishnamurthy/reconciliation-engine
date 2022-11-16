package com.taxreco.recon.engine.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.taxreco.recon.engine.model.ReconciliationTriggeredEvent
import com.taxreco.recon.engine.model.ReconciliationTriggeredForBucketEvent
import com.taxreco.recon.engine.service.ReconciliationService
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.StreamConfiguration
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration


@Configuration
class NatsConsumerConfig(
    private val connection: Connection,
    @Value("\${reconTriggerSubject}") private val reconTriggerSubject: String,
    @Value("\${reconBucketTriggerSubject}") private val reconBucketTriggerSubject: String,
    @Value("\${reconConsumerStreamName}") private val reconConsumerStreamName: String,
    @Value("\${reconTriggerSubjectDurable}") private val reconTriggerSubjectDurable: String,
    @Value("\${reconBucketTriggerSubjectDurable}") private val reconBucketTriggerSubjectDurable: String,
    private val reconciliationService: ReconciliationService
) {

    val jacksonObjectMapper = jacksonObjectMapper()

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @PostConstruct
    fun setup() {
        try {
            logger.info(" Setting up consumer streams ")
            connection.jetStreamManagement().addStream(
                StreamConfiguration.Builder()
                    .name(reconConsumerStreamName)
                    .subjects(reconTriggerSubject, reconBucketTriggerSubject)
                    .build()
            )
        } catch (ex: Exception) {
            connection.jetStreamManagement().updateStream(
                StreamConfiguration.Builder()
                    .name(reconConsumerStreamName)
                    .subjects(reconTriggerSubject, reconBucketTriggerSubject)
                    .build()
            )
            logger.info(" Finished updating NATS Consumer for subject $reconTriggerSubject ")
        }

        reconTriggerEventSubscriber()
        reconTriggerBucketEventSubscriber()
    }

    private fun reconTriggerEventSubscriber() {
        val messageHandler: (Message) -> Unit = { msg ->
            reconciliationService.reconcile(
                jacksonObjectMapper.readValue(
                    msg.data,
                    ReconciliationTriggeredEvent::class.java
                )
            )
        }
        val push = PushSubscribeOptions.Builder().durable(reconTriggerSubjectDurable)
            .deliverGroup(reconTriggerSubject)
            .build()
        val dispatcher = connection.createDispatcher()
        connection.jetStream().subscribe(reconTriggerSubject, dispatcher, messageHandler, true, push)
    }

    private fun reconTriggerBucketEventSubscriber() {
        val messageHandler: (Message) -> Unit = { msg ->
            reconciliationService.reconcile(
                jacksonObjectMapper.readValue(
                    msg.data,
                    ReconciliationTriggeredForBucketEvent::class.java
                )
            )
        }
        val push = PushSubscribeOptions.Builder().durable(reconBucketTriggerSubjectDurable)
            .deliverGroup(reconBucketTriggerSubject)
            .build()
        val dispatcher = connection.createDispatcher()
        connection.jetStream().subscribe(reconBucketTriggerSubject, dispatcher, messageHandler, true, push)
    }
}