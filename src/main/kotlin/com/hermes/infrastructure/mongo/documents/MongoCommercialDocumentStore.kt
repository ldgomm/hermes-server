package com.hermes.infrastructure.mongo.documents

import com.hermes.application.documents.CommercialDocumentNumberGenerator
import com.hermes.application.documents.CommercialDocumentRepository
import com.hermes.application.documents.CommercialDocumentSearchQuery
import com.hermes.domain.document.CommercialDocument
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import org.bson.Document
import org.bson.conversions.Bson
import java.time.Instant

class MongoCommercialDocumentStore(database: MongoDatabase) {
    private val documents = database.getCollection(MongoCollectionNames.COMMERCIAL_DOCUMENTS)
    private val counters = database.getCollection(MongoCollectionNames.COUNTERS)

    val documentRepository: CommercialDocumentRepository = MongoCommercialDocumentRepository(documents)
    val numberGenerator: CommercialDocumentNumberGenerator = MongoCommercialDocumentNumberGenerator(counters)
}

private class MongoCommercialDocumentRepository(
    private val collection: MongoCollection<Document>,
) : CommercialDocumentRepository {
    override fun create(document: CommercialDocument) {
        collection.insertOne(MongoCommercialDocumentMappers.toDocument(document))
    }

    override fun update(document: CommercialDocument) {
        collection.replaceOne(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, document.id),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, document.organizationId),
            ),
            MongoCommercialDocumentMappers.toDocument(document),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findById(organizationId: String, documentId: String): CommercialDocument? = collection.find(
        Filters.and(
            Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
            Filters.eq(MongoDocumentFields.ID, documentId.trim()),
        )
    ).firstOrNull()?.let(MongoCommercialDocumentMappers::fromDocument)

    override fun findBySale(organizationId: String, saleId: String): List<CommercialDocument> = collection.find(
        Filters.and(
            Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
            Filters.eq("saleId", saleId.trim()),
        )
    ).sort(Sorts.descending("issuedAt")).into(mutableListOf()).map(MongoCommercialDocumentMappers::fromDocument)

    override fun findByDocumentNumber(organizationId: String, documentNumber: String): CommercialDocument? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("documentNumber", documentNumber.trim()),
            )
        ).firstOrNull()?.let(MongoCommercialDocumentMappers::fromDocument)

    override fun search(query: CommercialDocumentSearchQuery): List<CommercialDocument> {
        val filters = mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()))
        query.saleId?.takeIf { it.isNotBlank() }?.let { filters += Filters.eq("saleId", it.trim()) }
        query.documentType?.let { filters += Filters.eq("documentType", it.storageValue) }
        if (query.statuses.isNotEmpty()) filters += Filters.`in`("status", query.statuses.map { it.name.lowercase() })
        query.from?.let { filters += Filters.gte("issuedAt", MongoInstantMapper.toDate(it)) }
        query.to?.let { filters += Filters.lte("issuedAt", MongoInstantMapper.toDate(it)) }

        return collection.find(Filters.and(filters)).sort(Sorts.descending("issuedAt"))
            .limit(query.limit.coerceIn(1, 500)).into(mutableListOf()).map(MongoCommercialDocumentMappers::fromDocument)
    }
}

private class MongoCommercialDocumentNumberGenerator(
    private val collection: MongoCollection<Document>,
) : CommercialDocumentNumberGenerator {
    override fun nextInternalTicketNumber(organizationId: String, branchId: String, issuedAt: Instant): String {
        val sequence = incrementAndGet(
            organizationId = organizationId.trim(),
            scope = "branch",
            scopeId = branchId.trim(),
            counterType = "internal_ticket",
            issuedAt = issuedAt,
        )
        return "TCK-${issuedAt.atOffset(java.time.ZoneOffset.UTC).toLocalDate()}-${
            sequence.toString().padStart(9, '0')
        }"
    }

    private fun incrementAndGet(
        organizationId: String,
        scope: String,
        scopeId: String,
        counterType: String,
        issuedAt: Instant,
    ): Long {
        val now = MongoInstantMapper.toDate(issuedAt)
        val updated = collection.findOneAndUpdate(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId),
                Filters.eq("scope", scope),
                Filters.eq("scopeId", scopeId),
                Filters.eq("counterType", counterType),
            ),
            Updates.combine(
                Updates.setOnInsert(
                    MongoDocumentFields.ID,
                    "cnt_${organizationId}_${scope}_${scopeId}_${counterType}".replace(Regex("[^a-zA-Z0-9_-]"), "_")
                ),
                Updates.setOnInsert(MongoDocumentFields.ORGANIZATION_ID, organizationId),
                Updates.setOnInsert("scope", scope),
                Updates.setOnInsert("scopeId", scopeId),
                Updates.setOnInsert("counterType", counterType),
                Updates.setOnInsert("padding", 9),
                Updates.setOnInsert("prefix", "TCK"),
                Updates.setOnInsert("status", "active"),
                Updates.setOnInsert(MongoDocumentFields.CREATED_AT, now),
                Updates.setOnInsert(MongoDocumentFields.CREATED_BY, "system"),
                Updates.setOnInsert(MongoDocumentFields.SCHEMA_VERSION, 1),
                Updates.inc("current", 1),
                Updates.set("lastIssuedAt", now),
                Updates.set(MongoDocumentFields.UPDATED_AT, now),
                Updates.set(MongoDocumentFields.UPDATED_BY, "system"),
                Updates.inc(MongoDocumentFields.VERSION, 1),
            ),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
        ) ?: error("Could not increment commercial document counter.")

        return when (val raw = updated["current"]) {
            is Int -> raw.toLong()
            is Long -> raw
            is Number -> raw.toLong()
            else -> error("Counter current is not numeric.")
        }
    }
}
