package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class RoleRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.ROLES,
) {
    fun findOrganizationRoleByCode(organizationId: String, code: String): Document? = findOne(
        and(organizationFilter(organizationId), eq("code", code.trim())),
    )

    fun findPlatformRoleByCode(code: String): Document? = findOne(
        and(eq("scope", "platform"), eq("code", code.trim())),
    )
}
