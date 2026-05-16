package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class MembershipRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.MEMBERSHIPS,
) {
    fun findByOrganizationAndUser(organizationId: String, userId: String): Document? = findOne(
        and(organizationFilter(organizationId), eq("userId", userId)),
    )

    fun findActiveByOrganization(organizationId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("status", "active")),
    )

    fun findActiveByUser(userId: String): List<Document> = findMany(
        filter = and(eq("userId", userId), eq("status", "active")),
    )
}
