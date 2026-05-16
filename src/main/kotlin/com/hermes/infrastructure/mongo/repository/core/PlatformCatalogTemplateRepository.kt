package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.regex.Pattern

class PlatformCatalogTemplateRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES) {
    fun findByGlobalCatalogId(globalCatalogId: String): Document? =
        findOne(eq("globalCatalogId", globalCatalogId.trim()))

    fun findByIdentifier(normalizedValue: String): List<Document> =
        findMany(eq("identifiers.normalizedValue", normalizedValue.trim()))

    fun findPublishedByFamily(productFamilyId: String): List<Document> =
        findMany(and(eq("productFamilyId", productFamilyId), eq("status", "published")))

    fun searchByNormalizedName(query: String, limit: Int = DEFAULT_LIMIT): List<Document> = findMany(
        filter = regex("normalizedName", Pattern.quote(query.trim().lowercase()), "i"),
        sort = Sorts.ascending("canonicalName"),
        limit = limit,
    )
}
