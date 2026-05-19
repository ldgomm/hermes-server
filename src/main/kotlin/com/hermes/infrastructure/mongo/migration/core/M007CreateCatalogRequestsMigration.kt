package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes
import org.bson.Document

object M007CreateCatalogRequestsMigration : MongoMigration {
    override val id: String = "M007_create_catalog_requests"
    override val description: String = "Create catalog item requests collection."

    override fun up(database: MongoDatabase) {
        val properties = Document().append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append(MongoDocumentFields.ORGANIZATION_ID, MongoMigrationSupport.id(prefix = "org_"))
            .append("requestedByUserId", MongoMigrationSupport.id(prefix = "usr_"))
            .append("requestedName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedRequestedName", MongoMigrationSupport.string(maxLength = 256))
            .append("requestedType", MongoMigrationSupport.enum(CATALOG_ITEM_TYPES))
            .append("description", MongoMigrationSupport.nullableString(maxLength = 4096))
            .append("suggestedCategoryId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("suggestedTaxProfileCode", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("identifiers", MongoMigrationSupport.array())
            .append("status", MongoMigrationSupport.enum(CATALOG_REQUEST_STATUSES))
            .append("reviewedByUserId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("reviewedAt", MongoMigrationSupport.nullableDate())
            .append("reviewReason", MongoMigrationSupport.nullableString(maxLength = 2048))
            .append(MongoDocumentFields.CREATED_AT, MongoMigrationSupport.date())
            .append(MongoDocumentFields.UPDATED_AT, MongoMigrationSupport.date())
            .append(MongoDocumentFields.VERSION, MongoMigrationSupport.int())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.CATALOG_ITEM_REQUESTS,
            validator = MongoMigrationSupport.jsonSchema(
                required = listOf(
                    MongoDocumentFields.ID,
                    MongoDocumentFields.ORGANIZATION_ID,
                    "requestedByUserId",
                    "requestedName",
                    "normalizedRequestedName",
                    "requestedType",
                    "identifiers",
                    "status",
                    MongoDocumentFields.CREATED_AT,
                    MongoDocumentFields.UPDATED_AT,
                    MongoDocumentFields.VERSION,
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "status"),
            name = "catalog_item_requests_org_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("requestedByUserId", "status"),
            name = "catalog_item_requests_requested_by_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "normalizedRequestedName", "status"),
            name = "catalog_item_requests_org_normalized_name_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("status", "createdAt"),
            name = "catalog_item_requests_status_created_at_idx",
        )
    }

    private val CATALOG_ITEM_TYPES = listOf("PRODUCT", "SERVICE", "PACKAGE", "RENTAL", "FEE")
    private val CATALOG_REQUEST_STATUSES = listOf("PENDING_REVIEW", "APPROVED", "REJECTED", "CANCELED")
}
