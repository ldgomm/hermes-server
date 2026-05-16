package com.hermes.infrastructure.mongo.migration

import com.mongodb.client.MongoDatabase

interface MongoMigration {
    val id: String
    val description: String

    fun up(database: MongoDatabase)
}
