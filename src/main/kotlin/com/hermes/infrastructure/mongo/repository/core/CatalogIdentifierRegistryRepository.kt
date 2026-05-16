package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class CatalogIdentifierRegistryRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CATALOG_IDENTIFIER_REGISTRY) {
    fun findByIdentifier(type: String, normalizedValue: String): Document? =
        findOne(and(eq("type", type), eq("normalizedValue", normalizedValue)))

    fun findByGlobalCatalogId(globalCatalogId: String): List<Document> =
        findMany(eq("globalCatalogId", globalCatalogId))
}
