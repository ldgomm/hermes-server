package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class CredentialEventRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CREDENTIAL_EVENTS) {
    fun findByUser(userId: String, limit: Int = DEFAULT_LIMIT): List<Document> =
        findMany(eq("userId", userId), limit = limit)
}
