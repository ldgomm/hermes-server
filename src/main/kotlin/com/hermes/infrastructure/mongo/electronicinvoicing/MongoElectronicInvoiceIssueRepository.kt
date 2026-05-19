package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRepository
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import org.bson.Document

class MongoElectronicInvoiceIssueRepository(
    database: MongoDatabase,
) : ElectronicInvoiceIssueRepository {
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

    override fun existsAuthorizedInvoiceForSale(organizationId: String, saleId: String): Boolean =
        collection.countDocuments(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("saleId", saleId.trim()),
                Filters.eq("documentType", "electronic_invoice"),
                Filters.eq("status", ElectronicDocumentStatus.AUTHORIZED.name),
            )
        ) > 0
}