package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document

class AuditLogRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.AUDIT_LOGS) {
    fun findByEntity(
        organizationId: String,
        entityType: String,
        entityId: String,
    ): List<Document> = findMany(
        filter = Filters.and(
            organizationFilter(organizationId),
            Filters.eq("entityType", entityType),
            Filters.eq("entityId", entityId),
        ),
        sort = Sorts.descending("occurredAt"),
    )

    fun findByActor(
        organizationId: String,
        actorId: String,
    ): List<Document> = findMany(
        filter = Filters.and(
            organizationFilter(organizationId),
            Filters.eq("actorUserId", actorId),
        ),
        sort = Sorts.descending("occurredAt"),
    )
}
