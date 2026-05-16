package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class CatalogMediaAssetRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CATALOG_MEDIA_ASSETS) {
    fun findByOwner(ownerType: String, ownerId: String): List<Document> = findMany(
        filter = and(eq("ownerType", ownerType), eq("ownerId", ownerId)),
        sort = Sorts.ascending("sortOrder", "createdAt"),
    )

    fun findPrimaryByOwner(ownerType: String, ownerId: String): Document? =
        findOne(and(eq("ownerType", ownerType), eq("ownerId", ownerId), eq("isPrimary", true)))
}
