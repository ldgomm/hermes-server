package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class ReservationRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.RESERVATIONS) {
    fun findBySale(organizationId: String, saleId: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("saleId", saleId)))

    fun findByDateRange(organizationId: String, from: Date, to: Date): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), gte("startsAt", from), lte("startsAt", to)),
        sort = Sorts.ascending("startsAt"),
    )

    fun findUpcomingByCustomer(organizationId: String, customerId: String, from: Date): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("customerId", customerId), gte("startsAt", from)),
        sort = Sorts.ascending("startsAt"),
    )
}
