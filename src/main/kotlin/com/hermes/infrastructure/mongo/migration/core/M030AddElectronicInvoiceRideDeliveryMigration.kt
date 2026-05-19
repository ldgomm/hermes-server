package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.electronicinvoicing.ElectronicInvoicingMongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes
import org.bson.Document

object M030AddElectronicInvoiceRideDeliveryMigration : MongoMigration {
    override val id: String = "M030_add_electronic_invoice_ride_delivery"
    override val description: String =
        "Allow RIDE PDF artifacts for electronic invoice delivery."

    override fun up(database: MongoDatabase) {
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
                        "ride_pdf",
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
