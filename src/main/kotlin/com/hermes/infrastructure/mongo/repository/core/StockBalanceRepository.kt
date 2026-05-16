package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class StockBalanceRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.STOCK_BALANCES) {
    fun findByItemAndLocation(
        organizationId: String,
        catalogItemId: String,
        branchId: String,
        locationCode: String? = null
    ): Document? = findOne(
        and(
            organizationFilter(organizationId),
            eq("catalogItemId", catalogItemId),
            eq("branchId", branchId),
            eq("locationCode", locationCode)
        ),
    )

    fun findLowStock(organizationId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("status", "low_stock")))
}
