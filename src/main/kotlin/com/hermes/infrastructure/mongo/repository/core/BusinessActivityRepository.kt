package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class BusinessActivityRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.ORGANIZATION_ACTIVITIES,
) {
    fun findByCode(organizationId: String, code: String): Document? = findOne(
        and(organizationFilter(organizationId), eq("code", code.trim())),
    )

    fun findActiveByOrganization(organizationId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("status", "active")),
        sort = Sorts.ascending("sortOrder", "name"),
    )

    fun findByWorkflowMode(organizationId: String, workflowMode: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("workflowMode", workflowMode)),
        sort = Sorts.ascending("sortOrder", "name"),
    )
}
