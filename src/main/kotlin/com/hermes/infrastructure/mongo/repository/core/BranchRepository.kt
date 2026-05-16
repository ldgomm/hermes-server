package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document

class BranchRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.BRANCHES,
) {
    fun findMainBranch(organizationId: String): Document? = findOne(
        and(organizationFilter(organizationId), eq("type", "main"), eq("status", "active")),
    )

    fun findByCode(organizationId: String, code: String): Document? = findOne(
        and(organizationFilter(organizationId), eq("code", code.trim())),
    )

    fun findActiveByOrganization(organizationId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("status", "active")),
        sort = Sorts.ascending("type", "name"),
    )

    fun findNear(
        longitude: Double,
        latitude: Double,
        maxDistanceMeters: Double,
        limit: Int = DEFAULT_LIMIT
    ): List<Document> = findMany(
        filter = nearSphere("location.coordinates", longitude, latitude, maxDistanceMeters, 0.0),
        sort = Sorts.ascending("name"),
        limit = limit,
    )
}
