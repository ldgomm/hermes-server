package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document

class ReceivableRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.RECEIVABLES) {
    fun findOpenByOrganization(organizationId: String): List<Document> = findMany(
        filter = Filters.and(
            organizationFilter(organizationId),
            Filters.`in`("status", listOf("open", "partially_collected", "overdue")),
        ),
        sort = Sorts.ascending("dueAt", "createdAt"),
    )

    fun findByCustomer(
        organizationId: String,
        customerId: String,
    ): List<Document> =
        findMany(Filters.and(organizationFilter(organizationId), Filters.eq("customerId", customerId)))

    fun findBySale(
        organizationId: String,
        saleId: String,
    ): Document? =
        findOne(Filters.and(organizationFilter(organizationId), Filters.eq("saleId", saleId)))
}
