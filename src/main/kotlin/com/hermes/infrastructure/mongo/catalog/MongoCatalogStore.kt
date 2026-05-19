package com.hermes.infrastructure.mongo.catalog

import com.hermes.application.catalog.*
import com.hermes.domain.catalog.*
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.time.Instant

class MongoCatalogStore(database: MongoDatabase) {
    val templateRepository: PlatformCatalogTemplateRepository = MongoPlatformCatalogTemplateRepository(database)
    val organizationItemRepository: OrganizationCatalogItemRepository = MongoOrganizationCatalogItemRepository(database)
    val requestRepository: CatalogItemRequestRepository = MongoCatalogItemRequestRepository(database)
    val requestSearchRepository: CatalogItemRequestSearchRepository = MongoCatalogItemRequestRepository(database)
    val priceHistoryRepository: CatalogPriceHistoryRepository = MongoCatalogPriceHistoryRepository(database)
    val identifierConflictChecker: CatalogIdentifierConflictChecker = MongoCatalogIdentifierConflictChecker(database)
    val taxProfileRepository: OrganizationCatalogTaxProfileRepository =
        MongoOrganizationCatalogTaxProfileRepository(database)
}

private class MongoPlatformCatalogTemplateRepository(database: MongoDatabase) : PlatformCatalogTemplateRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES)

    override fun create(template: PlatformCatalogTemplate) {
        collection.insertOne(MongoCatalogMappers.templateToDocument(template))
    }

    override fun update(template: PlatformCatalogTemplate) {
        collection.replaceOne(
            eq("_id", template.id), MongoCatalogMappers.templateToDocument(template), ReplaceOptions().upsert(false)
        )
    }

    override fun findById(id: String): PlatformCatalogTemplate? =
        collection.find(eq("_id", id.trim())).firstOrNull()?.let(MongoCatalogMappers::templateFromDocument)

    override fun existsByGlobalCatalogId(globalCatalogId: String): Boolean =
        collection.find(eq("globalCatalogId", globalCatalogId.trim().lowercase())).limit(1).firstOrNull() != null

    override fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate> {
        val filters = mutableListOf<org.bson.conversions.Bson>()
        if (query.onlyActive) filters += eq("status", CatalogTemplateStatus.ACTIVE.name)
        query.type?.let { filters += eq("type", it.name) }
        query.identifier?.takeIf { it.isNotBlank() }?.let { filters += eq("identifiers.normalizedValue", it) }
        query.query?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            filters += or(
                regex("canonicalName", text, "i"),
                regex("normalizedName", text.lowercase(), "i"),
                regex("globalCatalogId", text.lowercase(), "i")
            )
        }
        val finalFilter = if (filters.isEmpty()) Document() else and(filters)
        return collection.find(finalFilter).sort(Sorts.ascending("canonicalName")).limit(query.limit.coerceIn(1, 100))
            .into(mutableListOf()).map(MongoCatalogMappers::templateFromDocument)
    }
}

private class MongoOrganizationCatalogItemRepository(database: MongoDatabase) : OrganizationCatalogItemRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS)

    override fun create(item: OrganizationCatalogItem) {
        collection.insertOne(MongoCatalogMappers.itemToDocument(item))
    }

    override fun update(item: OrganizationCatalogItem) {
        collection.replaceOne(
            and(eq("_id", item.id), eq("organizationId", item.organizationId)),
            MongoCatalogMappers.itemToDocument(item),
            ReplaceOptions().upsert(false)
        )
    }

    override fun findById(organizationId: String, catalogItemId: String): OrganizationCatalogItem? =
        collection.find(and(eq("_id", catalogItemId.trim()), eq("organizationId", organizationId.trim()))).firstOrNull()
            ?.let(MongoCatalogMappers::itemFromDocument)

    override fun existsByTemplateId(organizationId: String, templateId: String): Boolean = collection.find(
        and(
            eq("organizationId", organizationId.trim()),
            eq("templateId", templateId.trim()),
            ne("status", CatalogItemStatus.REMOVED_FROM_ACCOUNT.name)
        )
    ).limit(1).firstOrNull() != null

    override fun search(query: OrganizationCatalogSearchQuery): List<OrganizationCatalogItem> {
        val filters = mutableListOf<org.bson.conversions.Bson>(eq("organizationId", query.organizationId.trim()))
        query.type?.let { filters += eq("type", it.name) }
        if (query.statuses.isNotEmpty()) filters += com.mongodb.client.model.Filters.`in`(
            "status", query.statuses.map { it.name })
        query.identifier?.takeIf { it.isNotBlank() }?.let { filters += eq("identifiers.normalizedValue", it) }
        query.query?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            filters += or(regex("localName", text, "i"), regex("searchableText", text.lowercase(), "i"))
        }
        return collection.find(and(filters)).sort(Sorts.ascending("localName")).limit(query.limit.coerceIn(1, 100))
            .into(mutableListOf()).map(MongoCatalogMappers::itemFromDocument)
    }
}

private class MongoCatalogItemRequestRepository(database: MongoDatabase) : CatalogItemRequestRepository,
    CatalogItemRequestSearchRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.CATALOG_ITEM_REQUESTS)

    override fun create(request: CatalogItemRequest) {
        collection.insertOne(MongoCatalogMappers.requestToDocument(request))
    }

    override fun update(request: CatalogItemRequest) {
        collection.replaceOne(
            eq("_id", request.id), MongoCatalogMappers.requestToDocument(request), ReplaceOptions().upsert(false)
        )
    }

    override fun findById(requestId: String): CatalogItemRequest? =
        collection.find(eq("_id", requestId.trim())).firstOrNull()?.let(MongoCatalogMappers::requestFromDocument)

    override fun findPendingByOrganizationAndName(organizationId: String, requestedName: String): CatalogItemRequest? =
        collection.find(
            and(
                eq("organizationId", organizationId.trim()),
                eq("normalizedRequestedName", requestedName.trim().lowercase()),
                eq("status", CatalogItemRequestStatus.PENDING_REVIEW.name)
            )
        ).firstOrNull()?.let(MongoCatalogMappers::requestFromDocument)

    override fun search(query: CatalogItemRequestSearchQuery): List<CatalogItemRequest> {
        val filters = mutableListOf<org.bson.conversions.Bson>()
        query.organizationId?.trim()?.takeIf { it.isNotBlank() }?.let { filters += eq("organizationId", it) }
        if (query.statuses.isNotEmpty()) filters += com.mongodb.client.model.Filters.`in`(
            "status", query.statuses.map { it.name })
        query.requestedType?.let { filters += eq("requestedType", it.name) }
        query.requestedByUserId?.trim()?.takeIf { it.isNotBlank() }?.let { filters += eq("requestedByUserId", it) }
        query.query?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            filters += or(
                regex("requestedName", text, "i"),
                regex("normalizedRequestedName", text.lowercase(), "i"),
                regex("identifiers.normalizedValue", text.uppercase(), "i")
            )
        }
        val finalFilter = if (filters.isEmpty()) Document() else and(filters)
        return collection.find(finalFilter).sort(Sorts.descending("createdAt")).limit(query.limit.coerceIn(1, 300))
            .into(mutableListOf()).map(MongoCatalogMappers::requestFromDocument)
    }
}

private class MongoCatalogPriceHistoryRepository(database: MongoDatabase) : CatalogPriceHistoryRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.CATALOG_PRICE_HISTORY)

    override fun create(history: CatalogPriceHistory) {
        collection.insertOne(MongoCatalogMappers.priceHistoryToDocument(history))
    }
}

private class MongoCatalogIdentifierConflictChecker(database: MongoDatabase) : CatalogIdentifierConflictChecker {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS)

    override fun existsLocalIdentifier(
        organizationId: String, normalizedValue: String, excludeCatalogItemId: String?
    ): Boolean {
        val filters = mutableListOf<org.bson.conversions.Bson>(
            eq("organizationId", organizationId.trim()), eq("identifiers.normalizedValue", normalizedValue.trim())
        )
        excludeCatalogItemId?.takeIf { it.isNotBlank() }?.let { filters += ne("_id", it) }
        return collection.find(and(filters)).limit(1).firstOrNull() != null
    }
}

class MongoOrganizationCatalogTaxProfileRepository(database: MongoDatabase) : OrganizationCatalogTaxProfileRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS)

    override fun assignTaxProfile(
        organizationId: String, catalogItemId: String, taxProfileId: String, updatedAt: Instant
    ): CatalogTaxProfileAssignmentRecord {
        val current = collection.find(and(eq("_id", catalogItemId.trim()), eq("organizationId", organizationId.trim())))
            .firstOrNull() ?: throw DomainRuleViolation("Organization catalog item does not exist.")
        val previousTaxProfileId = current.getString("taxProfileId")
        current["taxProfileId"] = taxProfileId
        collection.replaceOne(
            and(eq("_id", catalogItemId.trim()), eq("organizationId", organizationId.trim())),
            current,
            ReplaceOptions().upsert(false)
        )
        return CatalogTaxProfileAssignmentRecord(
            organizationId = organizationId,
            catalogItemId = catalogItemId,
            previousTaxProfileId = previousTaxProfileId,
            taxProfileId = taxProfileId,
            updatedAt = updatedAt,
        )
    }
}
