package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class ServiceAreaRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.SERVICE_AREAS) {
    fun findActiveByBranch(organizationId: String, branchId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("branchId", branchId), eq("status", "active")))

    fun findActiveByActivity(organizationId: String, activityId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("activityId", activityId), eq("status", "active")))
}
