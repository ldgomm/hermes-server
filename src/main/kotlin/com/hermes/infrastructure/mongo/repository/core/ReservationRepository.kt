package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class ReservationRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.RESERVATIONS) {
    fun findBySale(
        organizationId: String,
        saleId: String,
    ): Document? = findOne(Filters.and(organizationFilter(organizationId), Filters.eq("saleId", saleId)))

    fun findByDateRange(
        organizationId: String,
        from: Date,
        to: Date,
    ): List<Document> = findMany(
        filter = Filters.and(
            organizationFilter(organizationId),
            Filters.gte("startAt", from),
            Filters.lte("startAt", to),
        ),
        sort = Sorts.ascending("startAt"),
    )

    fun findUpcomingByCustomer(
        organizationId: String,
        customerId: String,
        from: Date,
    ): List<Document> = findMany(
        filter = Filters.and(
            organizationFilter(organizationId),
            Filters.eq("customerId", customerId),
            Filters.gte("startAt", from),
        ),
        sort = Sorts.ascending("startAt"),
    )
}
