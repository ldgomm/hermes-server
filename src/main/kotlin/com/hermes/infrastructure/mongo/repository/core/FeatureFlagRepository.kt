package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class FeatureFlagRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.FEATURE_FLAGS) {
    fun findByKey(key: String, organizationId: String? = null): Document? {
        val organizationFilter =
            if (organizationId == null) Document("organizationId", null) else eq("organizationId", organizationId)
        return findOne(and(eq("key", key), organizationFilter))
    }

    fun findEnabledForOrganization(organizationId: String): List<Document> =
        findMany(and(eq("organizationId", organizationId), eq("enabled", true)))
}
