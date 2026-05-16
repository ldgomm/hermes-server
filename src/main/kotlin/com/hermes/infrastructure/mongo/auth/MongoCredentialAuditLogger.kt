package com.hermes.infrastructure.mongo.auth

import com.hermes.application.auth.CredentialAuditEvent
import com.hermes.application.auth.CredentialAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase

class MongoCredentialAuditLogger(
    database: MongoDatabase,
) : CredentialAuditLogger {
    private val credentialEvents = database.getCollection(MongoCollectionNames.CREDENTIAL_EVENTS)

    override fun log(event: CredentialAuditEvent) {
        credentialEvents.insertOne(
            MongoAuthMappers.auditEventToDocument(
                action = event.action.name,
                actorUserId = event.actorUserId,
                targetUserId = event.targetUserId,
                organizationId = event.organizationId,
                sessionId = event.sessionId,
                ipAddress = event.ipAddress,
                userAgent = event.userAgent,
                message = event.message,
                createdAt = event.createdAt,
            ),
        )
    }
}
