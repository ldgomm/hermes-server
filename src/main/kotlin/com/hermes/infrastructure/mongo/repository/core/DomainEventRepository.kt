package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class DomainEventRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.DOMAIN_EVENTS) {
    fun findByAggregate(organizationId: String, aggregateType: String, aggregateId: String): List<Document> = findMany(
        filter = and(
            organizationFilter(organizationId),
            eq("aggregateType", aggregateType),
            eq("aggregateId", aggregateId)
        ),
        sort = Sorts.ascending("sequence"),
    )

    fun findByEventType(eventType: String, limit: Int = DEFAULT_LIMIT): List<Document> =
        findMany(eq("eventType", eventType), limit = limit)
}
