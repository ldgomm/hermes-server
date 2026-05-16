package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class StockMovementRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.STOCK_MOVEMENTS) {
    fun findByCatalogItem(organizationId: String, catalogItemId: String): List<Document> = findMany(
        and(organizationFilter(organizationId), eq("catalogItemId", catalogItemId)),
        sort = Sorts.descending("occurredAt")
    )

    fun findByReference(organizationId: String, referenceType: String, referenceId: String): List<Document> = findMany(
        and(
            organizationFilter(organizationId),
            eq("referenceType", referenceType),
            eq("referenceId", referenceId)
        )
    )
}
