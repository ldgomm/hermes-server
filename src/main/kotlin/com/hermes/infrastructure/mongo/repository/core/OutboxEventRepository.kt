package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class OutboxEventRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.OUTBOX_EVENTS) {
    fun findReady(now: Date, limit: Int = DEFAULT_LIMIT): List<Document> = findMany(
        filter = and(eq("status", "pending"), lte("availableAt", now)),
        sort = Sorts.ascending("availableAt", "createdAt"),
        limit = limit,
    )

    fun findByEventId(eventId: String): Document? = findOne(eq("eventId", eventId))
}
