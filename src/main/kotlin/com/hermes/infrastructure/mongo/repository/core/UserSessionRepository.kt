package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class UserSessionRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.USER_SESSIONS) {
    fun findActiveByUser(userId: String): List<Document> = findMany(and(eq("userId", userId), eq("status", "active")))
}
