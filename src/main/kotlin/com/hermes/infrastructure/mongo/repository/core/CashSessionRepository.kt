package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class CashSessionRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CASH_SESSIONS) {
    fun findOpenByBranch(organizationId: String, branchId: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("branchId", branchId), eq("status", "open")))

    fun findByOpenedBy(organizationId: String, openedBy: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("openedBy", openedBy)))
}
