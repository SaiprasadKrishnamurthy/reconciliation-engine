package com.taxreco.recon.engine.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory

open class MultiTenantMongoDBFactory(
    mongoClient: MongoClient,
    private val globalDB: String,
    private val tenantContext: TenantContext
) :
    SimpleMongoClientDatabaseFactory(
        mongoClient, globalDB
    ) {
    override fun getMongoDatabase(): MongoDatabase {
        return mongoClient.getDatabase(tenantDatabase)
    }

    private val tenantDatabase: String
        get() {
            val tenantId = tenantContext.tenantId
            return tenantId ?: globalDB
        }
}