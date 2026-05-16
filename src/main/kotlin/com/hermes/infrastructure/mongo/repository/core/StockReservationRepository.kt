package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class StockReservationRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.STOCK_RESERVATIONS) {
    fun findActiveBySale(organizationId: String, saleId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("saleId", saleId), eq("status", "active")))

    fun findActiveByItem(organizationId: String, catalogItemId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("catalogItemId", catalogItemId), eq("status", "active")))
}
