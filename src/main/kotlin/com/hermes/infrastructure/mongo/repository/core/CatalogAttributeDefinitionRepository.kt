package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class CatalogAttributeDefinitionRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CATALOG_ATTRIBUTE_DEFINITIONS) {
    fun findByCategoryAndKey(categoryCode: String, key: String): Document? =
        findOne(and(eq("categoryCode", categoryCode), eq("key", key)))

    fun findFilterableByCategory(categoryCode: String): List<Document> =
        findMany(and(eq("categoryCode", categoryCode), eq("filterable", true)))
}
