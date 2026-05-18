package com.hermes.infrastructure.mongo.catalog

import com.hermes.application.catalog.CatalogAuditAction
import com.hermes.application.catalog.CatalogAuditQuery
import com.hermes.application.catalog.CatalogAuditQueryRepository
import com.hermes.application.catalog.CatalogAuditRecord
import com.hermes.application.catalog.CatalogPriceHistoryQuery
import com.hermes.application.catalog.CatalogPriceHistoryQueryRepository
import com.hermes.domain.catalog.CatalogPriceHistory
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoDecimalMapper
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson

class MongoCatalogReadStore(database: MongoDatabase) {
    val auditQueryRepository: CatalogAuditQueryRepository = MongoCatalogAuditQueryRepository(database)
    val priceHistoryQueryRepository: CatalogPriceHistoryQueryRepository = MongoCatalogPriceHistoryQueryRepository(database)
}

private class MongoCatalogAuditQueryRepository(database: MongoDatabase) : CatalogAuditQueryRepository {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun search(query: CatalogAuditQuery): List<CatalogAuditRecord> {
        val filters = mutableListOf<Bson>(
            eq("module", "catalog"),
            eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()),
        )
        if (query.actions.isNotEmpty()) filters += `in`("action", query.actions.map { it.name })
        query.actorUserId?.trim()?.takeIf { it.isNotBlank() }?.let { filters += eq("actorUserId", it) }
        query.targetId?.trim()?.takeIf { it.isNotBlank() }?.let { filters += eq("targetId", it) }
        query.from?.let { filters += gte(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(it)) }
        query.to?.let { filters += lte(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(it)) }

        return collection.find(and(filters))
            .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
            .limit(query.limit.coerceIn(1, CatalogAuditQuery.MAX_LIMIT))
            .into(mutableListOf())
            .map(::auditFromDocument)
    }

    private fun auditFromDocument(document: Document): CatalogAuditRecord = CatalogAuditRecord(
        id = document.requiredString(MongoDocumentFields.ID),
        action = enumValueOf<CatalogAuditAction>(document.requiredString("action")),
        actorUserId = document.optionalString("actorUserId"),
        organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
        targetId = document.optionalString("targetId") ?: document.optionalString("entityId"),
        before = document.nullableStringMap("before"),
        after = document.nullableStringMap("after"),
        reason = document.optionalString("reason"),
        createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
    )
}

private class MongoCatalogPriceHistoryQueryRepository(database: MongoDatabase) : CatalogPriceHistoryQueryRepository {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.CATALOG_PRICE_HISTORY)

    override fun search(query: CatalogPriceHistoryQuery): List<CatalogPriceHistory> {
        val filters = mutableListOf<Bson>(
            eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()),
            eq("catalogItemId", query.catalogItemId.trim()),
        )
        query.from?.let { filters += gte("changedAt", MongoInstantMapper.toDate(it)) }
        query.to?.let { filters += lte("changedAt", MongoInstantMapper.toDate(it)) }

        return collection.find(and(filters))
            .sort(Sorts.descending("changedAt"))
            .limit(query.limit.coerceIn(1, CatalogPriceHistoryQuery.MAX_LIMIT))
            .into(mutableListOf())
            .map(::priceHistoryFromDocument)
    }

    private fun priceHistoryFromDocument(document: Document): CatalogPriceHistory = CatalogPriceHistory(
        id = document.requiredString(MongoDocumentFields.ID),
        organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
        catalogItemId = document.requiredString("catalogItemId"),
        oldPrice = moneyFromDocument(document.get("oldPrice", Document::class.java)),
        newPrice = moneyFromDocument(document.get("newPrice", Document::class.java)),
        changedByUserId = document.requiredString("changedByUserId"),
        reason = document.requiredString("reason"),
        changedAt = MongoInstantMapper.readRequired(document, "changedAt"),
    )

    private fun moneyFromDocument(document: Document): Money = Money.of(
        amount = MongoDecimalMapper.readRequired(document, "amount"),
        currency = CurrencyCode(document.requiredString("currency")),
    )
}

private fun Document.requiredString(field: String): String =
    getString(field)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

private fun Document.optionalString(field: String): String? =
    getString(field)?.takeIf { it.isNotBlank() }

private fun Document.nullableStringMap(field: String): Map<String, String?> =
    (get(field, Document::class.java) ?: Document()).mapValues { (_, value) -> value?.toString() }
