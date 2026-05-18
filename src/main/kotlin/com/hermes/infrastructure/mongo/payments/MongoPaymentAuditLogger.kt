package com.hermes.infrastructure.mongo.payments

import com.hermes.application.payments.PaymentAuditEvent
import com.hermes.application.payments.PaymentAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.util.UUID

class MongoPaymentAuditLogger(database: MongoDatabase) : PaymentAuditLogger {
    private val collection = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun log(event: PaymentAuditEvent) {
        val occurredAt = MongoInstantMapper.toDate(event.createdAt)
        collection.insertOne(
            Document(MongoDocumentFields.ID, "paudit_" + UUID.randomUUID().toString().replace("-", ""))
                .append(MongoDocumentFields.ORGANIZATION_ID, event.organizationId)
                .append(MongoDocumentFields.CREATED_AT, occurredAt)
                .append(MongoDocumentFields.CREATED_BY, event.actorUserId)
                .append(MongoDocumentFields.UPDATED_AT, occurredAt)
                .append(MongoDocumentFields.UPDATED_BY, event.actorUserId)
                .append(MongoDocumentFields.VERSION, 1)
                .append(MongoDocumentFields.SCHEMA_VERSION, 1)
                .append("module", "payments")
                .append("context", "payments")
                .append("actorUserId", event.actorUserId)
                .append("action", event.action.name)
                .append("entityType", "payment_cash_receivable")
                .append("entityId", event.targetId)
                .append("targetId", event.targetId)
                .append("saleId", event.saleId)
                .append("occurredAt", occurredAt)
                .append("reason", event.reason)
                .append("before", Document(event.before))
                .append("after", Document(event.after))
                .append("metadata", Document("module", "payments")),
        )
    }
}
