package com.taxreco.recon.engine.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.taxreco.recon.engine.model.ReconciliationTriggeredEvent
import com.taxreco.recon.engine.model.ReconciliationTriggeredForBucketEvent
import com.taxreco.recon.engine.service.ReconciliationService
import io.nats.client.*
import io.nats.client.api.StreamConfiguration
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration


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
    }

    @Scheduled(initialDelay = 5000, fixedDelayString = "\${recon.trigger.listener.poll.frequency.millis}")
    fun startReconTriggerConsumer() {
        val pullOptions = PullSubscribeOptions.builder()
            .durable(reconTriggerSubjectDurable)
            .build()

        val sub: JetStreamSubscription = connection
            .jetStream()
            .subscribe(reconTriggerSubject, pullOptions)
        val message: List<Message> = sub.fetch(100, Duration.ofSeconds(1))
        message.forEach { msg ->
            try {
                reconciliationService.reconcile(
                    jacksonObjectMapper.readValue(
                        msg.data,
                        ReconciliationTriggeredEvent::class.java
                    )
                )
            } finally {
                msg.ack()
            }
        }
    }

    @Scheduled(initialDelay = 5000, fixedDelayString = "\${recon.bucket.trigger.listener.poll.frequency.millis}")
    fun startReconBucketTriggerConsumer() {
        val pullOptions = PullSubscribeOptions.builder()
            .durable(reconBucketTriggerSubjectDurable)
            .build()

        val sub: JetStreamSubscription = connection.jetStream().subscribe(reconBucketTriggerSubject, pullOptions)
        val message: List<Message> = sub.fetch(100, Duration.ofSeconds(1))
        message.forEach { msg ->
            try {
                reconciliationService.reconcile(
                    jacksonObjectMapper.readValue(
                        msg.data,
                        ReconciliationTriggeredForBucketEvent::class.java
                    )
                )
            } finally {
                msg.ack()
            }
        }
    }
}