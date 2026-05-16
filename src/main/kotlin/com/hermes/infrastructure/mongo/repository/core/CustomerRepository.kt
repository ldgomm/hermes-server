package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import org.bson.Document
import java.util.regex.Pattern

class CustomerRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.CUSTOMERS) {
    fun findByIdentity(organizationId: String, idType: String, idNumber: String): Document? = findOne(
        and(
            organizationFilter(organizationId),
            eq("identity.idType", idType),
            eq("identity.idNumber", idNumber.trim())
        ),
    )

    fun searchByDisplayName(organizationId: String, query: String, limit: Int = DEFAULT_LIMIT): List<Document> =
        findMany(
            filter = and(organizationFilter(organizationId), regex("displayName", Pattern.quote(query.trim()), "i")),
            limit = limit,
        )
}
