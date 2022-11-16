package com.taxreco.recon.engine.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor


@Configuration
class AsyncConfig {
    @Bean("reconThreadPoolExecutor")
    fun getAsyncExecutor(
        @Value("\${corePoolSize}") corePoolSize: Int,
        @Value("\${maxPoolSize}") maxPoolSize: Int,
        @Value("\${queueCapacity}") queueCapacity: Int
    ): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = corePoolSize
        executor.maxPoolSize = maxPoolSize
        executor.queueCapacity = queueCapacity
        executor.setThreadNamePrefix("recon-")
        executor.initialize()
        return executor
    }
}