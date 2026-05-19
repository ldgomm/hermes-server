package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueAuditEvent
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.util.*

class MongoElectronicInvoiceIssueAuditLogger(
    database: MongoDatabase,
) : ElectronicInvoiceIssueAuditLogger {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun log(event: ElectronicInvoiceIssueAuditEvent) {
        val occurredAt = Date.from(event.createdAt)

        collection.insertOne(
            Document(MongoDocumentFields.ID, "eiaudit_" + UUID.randomUUID().toString().replace("-", "")).append(
                MongoDocumentFields.ORGANIZATION_ID, event.organizationId
            ).append(MongoDocumentFields.CREATED_AT, occurredAt)
                .append(MongoDocumentFields.CREATED_BY, event.actorUserId)
                .append(MongoDocumentFields.UPDATED_AT, occurredAt)
                .append(MongoDocumentFields.UPDATED_BY, event.actorUserId).append(MongoDocumentFields.VERSION, 1L)
                .append(MongoDocumentFields.SCHEMA_VERSION, 1).append("module", "electronic_invoicing")
                .append("context", "electronic_invoice_issue").append("actorUserId", event.actorUserId)
                .append("action", event.action.name).append("entityType", "electronic_invoice_issue")
                .append("entityId", event.documentId).append("targetId", event.documentId)
                .append("saleId", event.saleId).append("accessKey", event.accessKey)
                .append("status", event.status?.name).append("message", event.message).append("occurredAt", occurredAt)
                .append(
                    "metadata",
                    Document("documentId", event.documentId).append("saleId", event.saleId)
                        .append("accessKey", event.accessKey),
                ),
        )
    }
}