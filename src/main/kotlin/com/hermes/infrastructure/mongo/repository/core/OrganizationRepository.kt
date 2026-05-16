package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class OrganizationRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.ORGANIZATIONS,
) {
    fun findByTaxId(countryCode: String, taxId: String): Document? = findOne(
        and(eq("countryCode", countryCode.uppercase()), eq("taxId", taxId.trim())),
    )

    fun findByCommercialName(name: String, limit: Int = DEFAULT_LIMIT): List<Document> = findMany(
        filter = eq("commercialName", name.trim()),
        sort = Sorts.ascending("commercialName"),
        limit = limit,
    )

    fun findActive(limit: Int = DEFAULT_LIMIT): List<Document> = findByStatus("active", limit)
}
