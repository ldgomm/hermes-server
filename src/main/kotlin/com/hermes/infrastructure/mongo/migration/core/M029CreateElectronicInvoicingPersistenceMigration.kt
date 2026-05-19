package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.electronicinvoicing.ElectronicInvoicingMongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import org.bson.Document

object M029CreateElectronicInvoicingPersistenceMigration : MongoMigration {
    override val id: String = "M029_create_electronic_invoicing_persistence"
    override val description: String =
        "Create electronic invoice issue records and electronic document artifact metadata."

    override fun up(database: MongoDatabase) {
        createIssueRecords(database)
        createArtifactMetadata(database)
    }

    private fun createIssueRecords(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.string(maxLength = 128))
            .append("emissionPointId", MongoMigrationSupport.string(maxLength = 128))
            .append("saleId", MongoMigrationSupport.string(maxLength = 128))
            .append("environment", MongoMigrationSupport.enum(listOf("test", "production")))
            .append("documentType", MongoMigrationSupport.enum(listOf("electronic_invoice")))
            .append("establishmentCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("emissionPointCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("series", MongoMigrationSupport.string(minLength = 6, maxLength = 6))
            .append("documentNumber", MongoMigrationSupport.string(minLength = 17, maxLength = 17))
            .append("accessKey", MongoMigrationSupport.string(minLength = 49, maxLength = 49))
            .append("authorizationNumber", MongoMigrationSupport.string(minLength = 49, maxLength = 49))
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "DRAFT",
                        "READY_TO_ISSUE",
                        "ACCESS_KEY_GENERATED",
                        "XML_GENERATED",
                        "XSD_VALIDATED",
                        "XSD_INVALID",
                        "SIGNED",
                        "SIGNATURE_FAILED",
                        "SUBMITTED_TO_RECEPTION",
                        "RECEIVED_BY_SRI",
                        "RETURNED_BY_SRI",
                        "AUTHORIZATION_PENDING",
                        "AUTHORIZED",
                        "NOT_AUTHORIZED",
                        "DELIVERY_PENDING",
                        "DELIVERED",
                        "DELIVERY_FAILED",
                        "ERROR",
                        "CANCELLATION_REQUESTED",
                        "CANCELED",
                    )
                )
            )
            .append("schemaVersionCode", MongoMigrationSupport.nullableString(maxLength = 32))
            .append("unsignedXmlObjectKey", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append("unsignedXmlSha256", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("signedXmlObjectKey", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append("signedXmlSha256", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("authorizedXmlObjectKey", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append("authorizedXmlSha256", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("signatureId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("lastSriReceptionStatus", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("lastSriAuthorizationStatus", MongoMigrationSupport.nullableString(maxLength = 64))
            .append(
                "sriMessages",
                MongoMigrationSupport.array(
                    MongoMigrationSupport.obj(
                        properties = Document()
                            .append("identifier", MongoMigrationSupport.nullableString(maxLength = 64))
                            .append("message", MongoMigrationSupport.string(maxLength = 4096))
                            .append("additionalInfo", MongoMigrationSupport.nullableString(maxLength = 4096))
                            .append("type", MongoMigrationSupport.enum(listOf("INFO", "WARNING", "ERROR"))),
                        required = listOf("message", "type"),
                    )
                )
            )
            .append(
                "lastErrorClassification",
                Document("bsonType", listOf("object", "null"))
                    .append(
                        "properties",
                        Document()
                            .append("category", MongoMigrationSupport.string(maxLength = 64))
                            .append("recoverability", MongoMigrationSupport.string(maxLength = 128))
                            .append("userActionRequired", MongoMigrationSupport.bool())
                            .append("shouldKeepSameAccessKey", MongoMigrationSupport.bool())
                            .append("reason", MongoMigrationSupport.string(maxLength = 4096))
                    )
            )
            .append("issuedAt", MongoMigrationSupport.date())
            .append("authorizedAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = ElectronicInvoicingMongoCollectionNames.ELECTRONIC_INVOICE_ISSUES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    MongoDocumentFields.CREATED_BY,
                    MongoDocumentFields.UPDATED_BY,
                    "branchId",
                    "emissionPointId",
                    "saleId",
                    "environment",
                    "documentType",
                    "establishmentCode",
                    "emissionPointCode",
                    "series",
                    "documentNumber",
                    "accessKey",
                    "authorizationNumber",
                    "status",
                    "issuedAt",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, MongoDocumentFields.ID),
            name = "electronic_invoice_issues_org_id_unique_idx",
            unique = true,
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "accessKey"),
            name = "electronic_invoice_issues_org_access_key_unique_idx",
            unique = true,
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "saleId", "status"),
            name = "electronic_invoice_issues_org_sale_status_idx",
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "saleId"),
            name = "electronic_invoice_issues_one_authorized_per_sale_idx",
            unique = true,
            partialFilterExpression = Filters.eq("status", "AUTHORIZED"),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "status", "issuedAt"),
            name = "electronic_invoice_issues_org_status_issued_idx",
        )
    }

    private fun createArtifactMetadata(database: MongoDatabase) {
        val sizeBytesSchema = Document("bsonType", listOf("int", "long"))

        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("documentId", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "artifactType",
                MongoMigrationSupport.enum(
                    listOf(
                        "unsigned_xml",
                        "signed_xml",
                        "authorized_xml",
                        "sri_reception_request",
                        "sri_reception_response",
                        "sri_authorization_request",
                        "sri_authorization_response",
                    )
                )
            )
            .append("objectKey", MongoMigrationSupport.string(maxLength = 1024))
            .append("sha256", MongoMigrationSupport.string(minLength = 64, maxLength = 64))
            .append("sizeBytes", sizeBytesSchema)
            .append("contentType", MongoMigrationSupport.string(maxLength = 128))
            .append("fileName", MongoMigrationSupport.string(maxLength = 256))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = ElectronicInvoicingMongoCollectionNames.ELECTRONIC_DOCUMENT_ARTIFACTS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "documentId",
                    "artifactType",
                    "objectKey",
                    "sha256",
                    "sizeBytes",
                    "contentType",
                    "fileName",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "documentId", "artifactType"),
            name = "electronic_document_artifacts_org_document_type_idx",
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("objectKey"),
            name = "electronic_document_artifacts_object_key_unique_idx",
            unique = true,
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "sha256"),
            name = "electronic_document_artifacts_org_sha_idx",
        )
    }
}