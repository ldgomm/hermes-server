package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M015CreateElectronicSignaturesMigration : MongoMigration {
    override val id: String = "M015_create_electronic_signatures"
    override val description: String = "Create electronic signature vault metadata and signature usage events."

    override fun up(database: MongoDatabase) {
        createSignatures(database)
        createSignatureEvents(database)
    }

    private fun createSignatures(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("alias", MongoMigrationSupport.string(maxLength = 128))
            .append("certificateSubject", MongoMigrationSupport.string(maxLength = 512))
            .append("issuer", MongoMigrationSupport.nullableString(maxLength = 512))
            .append("serialNumber", MongoMigrationSupport.nullableString(maxLength = 256))
            .append("validFrom", MongoMigrationSupport.date())
            .append("validTo", MongoMigrationSupport.date())
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "uploaded",
                        "tested",
                        "active",
                        "expired",
                        "revoked",
                        "failed",
                        "archived"
                    )
                )
            )
            .append("encryptedFileObjectKey", MongoMigrationSupport.string(maxLength = 512))
            .append("encryptedPasswordRef", MongoMigrationSupport.string(maxLength = 512))
            .append("lastTestedAt", MongoMigrationSupport.nullableDate())
            .append("lastUsedAt", MongoMigrationSupport.nullableDate())
            .append("failureCount", MongoMigrationSupport.int())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.ELECTRONIC_SIGNATURES,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "alias",
                    "certificateSubject",
                    "validFrom",
                    "validTo",
                    "status",
                    "encryptedFileObjectKey",
                    "encryptedPasswordRef",
                    "failureCount",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "validTo"),
            "electronic_signatures_org_status_valid_to_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "alias"),
            "electronic_signatures_org_alias_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "serialNumber"),
            "electronic_signatures_org_serial_idx",
            sparse = true
        )
    }

    private fun createSignatureEvents(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("signatureId", MongoMigrationSupport.id(prefix = "sig_"))
            .append(
                "eventType",
                MongoMigrationSupport.enum(
                    listOf(
                        "uploaded",
                        "tested",
                        "activated",
                        "used_for_signing",
                        "failed",
                        "revoked",
                        "replaced",
                        "expired_detected"
                    )
                )
            )
            .append("documentId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("performedBy", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("occurredAt", MongoMigrationSupport.date())
            .append("message", MongoMigrationSupport.nullableString(maxLength = 2048))
            .append("metadata", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.ELECTRONIC_SIGNATURE_EVENTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "signatureId",
                    "eventType",
                    "occurredAt"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "signatureId", "occurredAt"),
            "signature_events_org_signature_occurred_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "documentId", "occurredAt"),
            "signature_events_org_document_occurred_idx",
            sparse = true
        )
    }
}
