package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class ServiceOrderRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.SERVICE_ORDERS) {
    fun findBySale(organizationId: String, saleId: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("saleId", saleId)))

    fun findByStatus(organizationId: String, status: String): List<Document> =
        findByOrganizationAndStatus(organizationId, status)
}
