package com.hermes.infrastructure.mongo.migration

import com.mongodb.client.MongoDatabase //Unresolved reference 'mongodb'.

interface MongoMigration {
    val id: String
    val description: String

    fun up(database: MongoDatabase) //Unresolved reference 'MongoDatabase'.
}
