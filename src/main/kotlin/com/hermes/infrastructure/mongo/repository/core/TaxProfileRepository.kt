package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class TaxProfileRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.TAX_PROFILES) {
    fun findGlobalByCode(code: String): Document? = findOne(
        and(eq("code", code.trim()), Document("organizationId", null)),
    ) ?: findOne(and(eq("code", code.trim()), eq("status", "active")))

    fun findByCodeWithOrganizationOverride(organizationId: String, code: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("code", code.trim()), eq("status", "active")))
            ?: findGlobalByCode(code)
}
