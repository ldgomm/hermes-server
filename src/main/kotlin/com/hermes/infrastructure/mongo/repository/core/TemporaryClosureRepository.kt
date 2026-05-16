package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import org.bson.Document
import java.util.*

class TemporaryClosureRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.TEMPORARY_CLOSURES) {
    fun findActiveOnDate(organizationId: String, branchId: String, date: Date): List<Document> = findMany(
        and(
            organizationFilter(organizationId),
            eq("branchId", branchId),
            lte("startsAt", date),
            gte("endsAt", date),
            eq("status", "active")
        ),
    )
}
