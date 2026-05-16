package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M027CreateCatalogImportJobsFutureMigration : MongoMigration {
    override val id: String = "M027_create_catalog_import_jobs_future"
    override val description: String =
        "Create future catalog import jobs with mapping, validation, matching and commit state."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("activityId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("uploadedBy", MongoMigrationSupport.id(prefix = "usr_"))
            .append("filename", MongoMigrationSupport.string(maxLength = 256))
            .append("contentType", MongoMigrationSupport.string(maxLength = 128))
            .append("objectKey", MongoMigrationSupport.string(maxLength = 512))
            .append("sizeBytes", MongoMigrationSupport.int())
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "uploaded",
                        "mapping_required",
                        "validating",
                        "matched",
                        "needs_review",
                        "ready_to_commit",
                        "committed",
                        "failed",
                        "canceled"
                    )
                )
            )
            .append("totalRows", MongoMigrationSupport.int())
            .append("validRows", MongoMigrationSupport.int())
            .append("errorRows", MongoMigrationSupport.int())
            .append("mapping", MongoMigrationSupport.obj())
            .append("summary", MongoMigrationSupport.obj())
            .append("committedAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CATALOG_IMPORT_JOBS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "uploadedBy",
                    "filename",
                    "contentType",
                    "objectKey",
                    "sizeBytes",
                    "status",
                    "totalRows",
                    "validRows",
                    "errorRows",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "createdAt"),
            "catalog_import_jobs_org_status_created_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "uploadedBy", "createdAt"),
            "catalog_import_jobs_org_uploaded_by_created_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("objectKey"),
            "catalog_import_jobs_object_key_unique_idx",
            unique = true
        )
    }
}
