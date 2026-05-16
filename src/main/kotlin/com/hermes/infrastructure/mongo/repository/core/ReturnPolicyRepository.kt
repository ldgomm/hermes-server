package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class ReturnPolicyRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.RETURN_POLICIES) {
    fun findActiveByScope(
        organizationId: String, scope: String, categoryCode: String? = null, catalogItemId: String? = null
    ): List<Document> = findMany(
        and(
            organizationFilter(organizationId),
            eq("scope", scope),
            eq("categoryCode", categoryCode),
            eq("catalogItemId", catalogItemId),
            eq("status", "active")
        ),
    )
}
