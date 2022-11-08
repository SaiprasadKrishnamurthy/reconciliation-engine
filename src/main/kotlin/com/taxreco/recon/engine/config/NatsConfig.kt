package com.taxreco.recon.engine.config

import io.nats.client.Connection
import io.nats.client.Nats
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NatsConfig {

    @Bean
    fun natsConnection(@Value("\${nats.server}") natsServer: String): Connection {
        return Nats.connect(natsServer)
    }
}