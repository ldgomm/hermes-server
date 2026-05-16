package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class CatalogPriceHistoryRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CATALOG_PRICE_HISTORY) {
    fun findByCatalogItem(organizationId: String, catalogItemId: String, limit: Int = DEFAULT_LIMIT): List<Document> =
        findMany(
            filter = and(organizationFilter(organizationId), eq("catalogItemId", catalogItemId)),
            sort = Sorts.descending("changedAt"),
            limit = limit,
        )

    fun findByGlobalCatalogId(organizationId: String, globalCatalogId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("globalCatalogId", globalCatalogId)),
        sort = Sorts.descending("changedAt"),
    )
}
