package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M014CreateCommercialDocumentsMigration : MongoMigration {
    override val id: String = "M014_create_commercial_documents"
    override val description: String = "Create commercial documents, electronic payloads and SRI submissions."

    override fun up(database: MongoDatabase) {
        createCommercialDocuments(database)
        createElectronicDocumentPayloads(database)
        createSriSubmissions(database)
    }

    private fun createCommercialDocuments(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("emissionPointId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("saleId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("customerId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append(
                "documentType",
                MongoMigrationSupport.enum(
                    listOf(
                        "internal_ticket",
                        "physical_sale_note_registry",
                        "electronic_invoice",
                        "credit_note",
                        "debit_note",
                        "withholding",
                        "remission_guide"
                    )
                )
            )
            .append("documentNumber", MongoMigrationSupport.string(maxLength = 64))
            .append("accessKey", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("authorizationNumber", MongoMigrationSupport.nullableString(maxLength = 64))
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "draft",
                        "generated",
                        "validated",
                        "signed",
                        "sent",
                        "received",
                        "authorized",
                        "rejected",
                        "returned",
                        "cancellation_requested",
                        "pending_cancellation",
                        "canceled",
                        "error"
                    )
                )
            )
            .append("issuedAt", MongoMigrationSupport.date())
            .append("authorizedAt", MongoMigrationSupport.nullableDate())
            .append("totalsSnapshot", MongoMigrationSupport.obj())
            .append("taxSnapshot", MongoMigrationSupport.obj())
            .append("payloadId", MongoMigrationSupport.nullableString(maxLength = 128))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.COMMERCIAL_DOCUMENTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "documentType",
                    "documentNumber",
                    "status",
                    "issuedAt",
                    "totalsSnapshot",
                    "taxSnapshot"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "documentType", "documentNumber"),
            "commercial_documents_org_type_number_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "saleId", "documentType"),
            "commercial_documents_org_sale_type_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "accessKey"),
            "commercial_documents_org_access_key_unique_idx",
            unique = true,
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "issuedAt"),
            "commercial_documents_org_status_issued_idx"
        )
    }

    private fun createElectronicDocumentPayloads(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("documentId", MongoMigrationSupport.id(prefix = "doc_"))
            .append("xmlUnsignedObjectKey", MongoMigrationSupport.nullableString(maxLength = 512))
            .append("xmlSignedObjectKey", MongoMigrationSupport.nullableString(maxLength = 512))
            .append("rideObjectKey", MongoMigrationSupport.nullableString(maxLength = 512))
            .append("schemaVersionCode", MongoMigrationSupport.string(maxLength = 32))
            .append("signatureId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("signedAt", MongoMigrationSupport.nullableDate())
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "draft",
                        "validated",
                        "signed",
                        "submitted",
                        "authorized",
                        "rejected",
                        "error"
                    )
                )
            )
            .append("checksums", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.ELECTRONIC_DOCUMENT_PAYLOADS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "documentId",
                    "schemaVersionCode",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "documentId"),
            "electronic_payloads_org_document_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "updatedAt"),
            "electronic_payloads_org_status_updated_idx"
        )
    }

    private fun createSriSubmissions(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("documentId", MongoMigrationSupport.id(prefix = "doc_"))
            .append("payloadId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("environment", MongoMigrationSupport.enum(listOf("test", "production")))
            .append("submissionType", MongoMigrationSupport.enum(listOf("reception", "authorization", "cancellation")))
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "pending",
                        "sent",
                        "received",
                        "authorized",
                        "rejected",
                        "returned",
                        "error"
                    )
                )
            )
            .append("requestAt", MongoMigrationSupport.date())
            .append("responseAt", MongoMigrationSupport.nullableDate())
            .append("responseCode", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("responseMessage", MongoMigrationSupport.nullableString(maxLength = 4096))
            .append("rawResponseObjectKey", MongoMigrationSupport.nullableString(maxLength = 512))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.SRI_SUBMISSIONS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "documentId",
                    "environment",
                    "submissionType",
                    "status",
                    "requestAt"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "documentId", "requestAt"),
            "sri_submissions_org_document_request_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "requestAt"),
            "sri_submissions_org_status_request_idx"
        )
    }
}
