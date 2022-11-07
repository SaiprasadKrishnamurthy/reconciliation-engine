package com.taxreco.recon.engine.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MongoConfigProperties {
    @Value("\${spring.data.mongodb.database:localDb}")
    var dataBaseName: String? = null
}