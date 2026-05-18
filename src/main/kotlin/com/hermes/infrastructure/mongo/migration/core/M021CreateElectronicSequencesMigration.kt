package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.electronicinvoicing.ElectronicSequenceMongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes

object M021CreateElectronicSequencesMigration : MongoMigration {
    override val id: String = "M021_create_electronic_sequences"
    override val description: String = "Create atomic SRI electronic document sequences."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("environment", MongoMigrationSupport.enum(listOf("test", "production")))
            .append(
                "documentType",
                MongoMigrationSupport.enum(
                    listOf(
                        "electronic_invoice",
                        "purchase_liquidation",
                        "credit_note",
                        "debit_note",
                        "remission_guide",
                        "withholding",
                    )
                )
            )
            .append("establishmentCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("emissionPointCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("series", MongoMigrationSupport.string(minLength = 6, maxLength = 6))
            .append("currentValue", MongoMigrationSupport.int())
            .append("lastIssuedDocumentId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("lastIssuedAt", MongoMigrationSupport.nullableDate())
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))
            .append(MongoDocumentFields.SCHEMA_VERSION, MongoMigrationSupport.int())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = ElectronicSequenceMongoCollectionNames.ELECTRONIC_SEQUENCES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "environment",
                    "documentType",
                    "establishmentCode",
                    "emissionPointCode",
                    "series",
                    "currentValue",
                    "status",
                    MongoDocumentFields.SCHEMA_VERSION,
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(
                MongoDocumentFields.ORGANIZATION_ID,
                "environment",
                "documentType",
                "establishmentCode",
                "emissionPointCode",
            ),
            name = "electronic_sequences_org_env_doc_series_unique_idx",
            unique = true,
            partialFilterExpression = Filters.ne("status", "archived"),
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "documentType", "status"),
            name = "electronic_sequences_org_doc_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "environment", "status"),
            name = "electronic_sequences_org_env_status_idx",
        )
    }
}
