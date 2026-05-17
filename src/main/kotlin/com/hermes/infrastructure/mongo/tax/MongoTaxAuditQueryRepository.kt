package com.hermes.infrastructure.mongo.tax

import com.hermes.application.tax.TaxAuditAction
import com.hermes.application.tax.TaxAuditQuery
import com.hermes.application.tax.TaxAuditQueryRepository
import com.hermes.application.tax.TaxAuditRecord
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document

class MongoTaxAuditQueryRepository(
    database: MongoDatabase,
) : TaxAuditQueryRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun search(query: TaxAuditQuery): List<TaxAuditRecord> {
        val filters = mutableListOf(
            eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()),
            eq("entityType", "tax"),
        )

        if (query.actions.isNotEmpty()) {
            filters += `in`("action", query.actions.map { it.name })
        }
        query.actorUserId?.takeIf { it.isNotBlank() }?.let { filters += eq("actorUserId", it.trim()) }
        query.targetId?.takeIf { it.isNotBlank() }?.let { filters += eq("metadata.targetId", it.trim()) }
        query.from?.let { filters += gte("occurredAt", MongoInstantMapper.toDate(it)) }
        query.to?.let { filters += lte("occurredAt", MongoInstantMapper.toDate(it)) }

        return collection
            .find(and(filters))
            .sort(Sorts.descending("occurredAt"))
            .limit(query.limit)
            .map(::toRecord)
            .toList()
    }

    private fun toRecord(document: Document): TaxAuditRecord =
        TaxAuditRecord(
            id = document.getString(MongoDocumentFields.ID),
            action = TaxAuditAction.valueOf(document.getString("action")),
            actorUserId = document.getString("actorUserId"),
            organizationId = document.getString(MongoDocumentFields.ORGANIZATION_ID),
            targetId = document.get("metadata", Document::class.java)?.getString("targetId"),
            before = document.get("before", Document::class.java).toStringMap(),
            after = document.get("after", Document::class.java).toStringMap(),
            reason = document.getString("reason"),
            createdAt = MongoInstantMapper.readRequired(document, "occurredAt"),
        )

    private fun Document?.toStringMap(): Map<String, String?> {
        if (this == null) return emptyMap()
        return entries.associate { (key, value) -> key to value?.toString() }
    }
}
