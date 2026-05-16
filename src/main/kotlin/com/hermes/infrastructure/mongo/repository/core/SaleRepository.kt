package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class SaleRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.SALES) {
    fun findBySaleNumber(organizationId: String, saleNumber: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("saleNumber", saleNumber.trim())))

    fun findOpenByOrganization(organizationId: String): List<Document> = findMany(
        filter = and(
            organizationFilter(organizationId),
            `in`("operationalStatus", listOf("draft", "pending", "confirmed", "in_progress", "ready", "delivered"))
        ),
        sort = Sorts.descending("createdAt"),
    )

    fun findByCustomer(organizationId: String, customerId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("customerId", customerId)))

    fun findByActivityAndDateRange(organizationId: String, activityId: String, from: Date, to: Date): List<Document> =
        findMany(
            filter = and(
                organizationFilter(organizationId),
                eq("activityId", activityId),
                com.mongodb.client.model.Filters.gte("createdAt", from),
                com.mongodb.client.model.Filters.lte("createdAt", to)
            ),
            sort = Sorts.descending("createdAt"),
        )
}
