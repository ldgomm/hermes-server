package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document

class ReceivableRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.RECEIVABLES) {
    fun findOpenByOrganization(organizationId: String): List<Document> = findMany(
        filter = and(
            organizationFilter(organizationId),
            `in`("status", listOf("pending", "partially_collected", "overdue"))
        ),
        sort = Sorts.ascending("dueAt", "createdAt"),
    )

    fun findByCustomer(organizationId: String, customerId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("customerId", customerId)))

    fun findBySale(organizationId: String, saleId: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("saleId", saleId)))
}
