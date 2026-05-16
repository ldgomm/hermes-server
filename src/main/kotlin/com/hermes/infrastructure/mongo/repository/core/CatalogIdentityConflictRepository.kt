package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class CatalogIdentityConflictRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CATALOG_IDENTITY_CONFLICTS) {
    fun findOpen(limit: Int = DEFAULT_LIMIT): List<Document> = findMany(eq("status", "open"), limit = limit)
    fun findByIdentifier(normalizedValue: String): List<Document> = findMany(eq("normalizedValue", normalizedValue))
}
