package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueQueryRepository
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRepository
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueSearchQuery
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson
import java.util.Date

class MongoElectronicInvoiceIssueRepository(
    database: MongoDatabase,
) : ElectronicInvoiceIssueRepository, ElectronicInvoiceIssueQueryRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(ElectronicInvoicingMongoCollectionNames.ELECTRONIC_INVOICE_ISSUES)

    override fun create(record: ElectronicInvoiceIssueRecord) {
        collection.insertOne(MongoElectronicInvoiceIssueMappers.toDocument(record))
    }

    override fun update(record: ElectronicInvoiceIssueRecord) {
        val result = collection.replaceOne(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, record.organizationId),
                Filters.eq(MongoDocumentFields.ID, record.id),
            ),
            MongoElectronicInvoiceIssueMappers.toDocument(record),
        )

        if (result.matchedCount != 1L) {
            throw IllegalStateException("Electronic invoice issue record ${record.id} does not exist.")
        }
    }

    override fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq(MongoDocumentFields.ID, documentId.trim()),
            )
        ).firstOrNull()?.let(MongoElectronicInvoiceIssueMappers::fromDocument)

    override fun search(query: ElectronicInvoiceIssueSearchQuery): List<ElectronicInvoiceIssueRecord> {
        val filters = mutableListOf<Bson>(
            Filters.eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()),
            Filters.eq("documentType", SriDocumentType.INVOICE.storageValue),
        )

        query.saleId?.trim()?.takeIf { it.isNotBlank() }?.let { saleId ->
            filters += Filters.eq("saleId", saleId)
        }
        if (query.statuses.isNotEmpty()) {
            filters += Filters.`in`("status", query.statuses.map { it.name })
        }
        query.environment?.let { environment ->
            filters += Filters.eq("environment", environment.storageValue)
        }
        query.from?.let { from ->
            filters += Filters.gte("issuedAt", Date.from(from))
        }
        query.to?.let { to ->
            filters += Filters.lte("issuedAt", Date.from(to))
        }

        return collection.find(Filters.and(filters))
            .sort(Sorts.descending("issuedAt", MongoDocumentFields.CREATED_AT, MongoDocumentFields.ID))
            .limit(query.limit)
            .into(mutableListOf())
            .map(MongoElectronicInvoiceIssueMappers::fromDocument)
    }

    override fun existsAuthorizedInvoiceForSale(organizationId: String, saleId: String): Boolean =
        collection.countDocuments(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("saleId", saleId.trim()),
                Filters.eq("documentType", "electronic_invoice"),
                Filters.`in`("status", authorizedDerivedStatuses),
            )
        ) > 0

    private companion object {
        val authorizedDerivedStatuses: List<String> = listOf(
            ElectronicDocumentStatus.AUTHORIZED.name,
            ElectronicDocumentStatus.DELIVERY_PENDING.name,
            ElectronicDocumentStatus.DELIVERED.name,
            ElectronicDocumentStatus.DELIVERY_FAILED.name,
        )
    }
}
