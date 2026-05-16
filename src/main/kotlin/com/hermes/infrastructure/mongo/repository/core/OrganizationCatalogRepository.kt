package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class OrganizationCatalogRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS,
) {
    fun findByLocalSku(organizationId: String, localSku: String): Document? = findOne(
        and(organizationFilter(organizationId), eq("localSku", localSku.trim().uppercase())),
    )

    fun findByIdentifier(organizationId: String, normalizedValue: String, limit: Int = DEFAULT_LIMIT): List<Document> =
        findMany(
            filter = and(organizationFilter(organizationId), eq("identifiers.normalizedValue", normalizedValue.trim())),
            sort = Sorts.ascending("localName"),
            limit = limit,
        )

    fun findActiveByActivity(organizationId: String, activityId: String, limit: Int = DEFAULT_LIMIT): List<Document> =
        findMany(
            filter = and(organizationFilter(organizationId), eq("activityId", activityId), eq("status", "active")),
            sort = Sorts.ascending("localName"),
            limit = limit,
        )

    fun findByGlobalCatalogId(organizationId: String, globalCatalogId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("globalCatalogId", globalCatalogId.trim())),
        sort = Sorts.ascending("localName"),
    )

    fun findPublicVisible(limit: Int = DEFAULT_LIMIT): List<Document> = findMany(
        filter = and(eq("publicDiscovery.visible", true), eq("publicDiscovery.allowSearch", true)),
        sort = Sorts.descending("updatedAt"),
        limit = limit,
    )
}
