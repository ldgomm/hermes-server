package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class CatalogRequestRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CATALOG_ITEM_REQUESTS) {
    fun findPendingForReview(limit: Int = DEFAULT_LIMIT): List<Document> =
        findMany(eq("status", "submitted"), limit = limit)

    fun findByRequestedBy(organizationId: String, requestedBy: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("requestedBy", requestedBy)))
}
