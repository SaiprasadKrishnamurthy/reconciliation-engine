package com.taxreco.recon.engine.config

import com.mongodb.ConnectionString
import com.mongodb.client.MongoClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.core.MongoClientFactoryBean
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


@Configuration
@EnableMongoRepositories(basePackages = ["com"])
class MongoMultiTenantConfig(private val tenantContext: TenantContext) : AbstractMongoClientConfiguration() {

    @Value("\${default.tenant}")
    private lateinit var defaultTenantDB: String

    @Profile("!local")
    @Bean
    fun mongo(
        @Value("\${mongodb.uri}") mongoUri: String
    ): MongoClientFactoryBean {
        val mongo = MongoClientFactoryBean()
        mongo.setConnectionString(ConnectionString(mongoUri))
        return mongo
    }

    @Bean
    @Primary
    fun mongoDbFactory(
        mongoConfigProperties: MongoConfigProperties,
        mongoClient: MongoClient,
        @Value("\${default.tenant}") defaultTenantDB: String,
    ): MongoDatabaseFactory {
        return MultiTenantMongoDBFactory(mongoClient, defaultTenantDB, tenantContext)
    }

    @Bean
    @Primary
    override fun mongoTemplate(mongoDbFactory: MongoDatabaseFactory, converter: MappingMongoConverter): MongoTemplate {
        return MongoTemplate(mongoDbFactory, converter)
    }

    override fun getDatabaseName(): String = defaultTenantDB
}