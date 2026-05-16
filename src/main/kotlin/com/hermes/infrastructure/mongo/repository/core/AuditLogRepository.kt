package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class AuditLogRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.AUDIT_LOGS) {
    fun findByEntity(organizationId: String, entityType: String, entityId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("entityType", entityType), eq("entityId", entityId)),
        sort = Sorts.descending("occurredAt"),
    )

    fun findByActor(organizationId: String, actorId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("actorId", actorId)), sort = Sorts.descending("occurredAt"))
}
