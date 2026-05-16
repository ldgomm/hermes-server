package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class CashMovementRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CASH_MOVEMENTS) {
    fun findByCashSession(organizationId: String, cashSessionId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("cashSessionId", cashSessionId)),
        sort = Sorts.ascending("occurredAt"),
    )

    fun findByReference(organizationId: String, referenceType: String, referenceId: String): List<Document> = findMany(
        and(
            organizationFilter(organizationId),
            eq("referenceType", referenceType),
            eq("referenceId", referenceId)
        )
    )
}
