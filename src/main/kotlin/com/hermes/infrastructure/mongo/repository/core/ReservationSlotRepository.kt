package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class ReservationSlotRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.RESERVATION_SLOTS) {
    fun findByResourceAndRange(organizationId: String, resourceId: String, from: Date, to: Date): List<Document> =
        findMany(
            filter = and(
                organizationFilter(organizationId),
                eq("resourceId", resourceId),
                gte("startsAt", from),
                lte("startsAt", to)
            ),
            sort = Sorts.ascending("startsAt"),
        )
}
