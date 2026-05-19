package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.electronicinvoicing.ElectronicInvoicingMongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes
import org.bson.Document

object M031CreateOrganizationSriSettingsMigration : MongoMigration {
    override val id: String = "M031_create_organization_sri_settings"
    override val description: String = "Create organization-level SRI electronic invoicing settings."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("environment", MongoMigrationSupport.enum(listOf("test", "production")))
            .append("ruc", MongoMigrationSupport.string(minLength = 13, maxLength = 13))
            .append("legalName", MongoMigrationSupport.string(maxLength = 300))
            .append("commercialName", MongoMigrationSupport.nullableString(maxLength = 300))
            .append("matrixAddress", MongoMigrationSupport.string(maxLength = 512))
            .append("establishmentAddress", MongoMigrationSupport.string(maxLength = 512))
            .append("establishmentCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("emissionPointCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("series", MongoMigrationSupport.string(minLength = 6, maxLength = 6))
            .append("invoiceSchemaVersion", MongoMigrationSupport.string(maxLength = 16))
            .append("invoiceSchemaVersionCode", MongoMigrationSupport.string(maxLength = 64))
            .append("specialTaxpayerCode", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("obligatedToKeepAccounting", MongoMigrationSupport.bool())
            .append("rimpeLegend", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("productionEnabled", MongoMigrationSupport.bool())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = ElectronicInvoicingMongoCollectionNames.ORGANIZATION_SRI_SETTINGS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "environment",
                    "ruc",
                    "legalName",
                    "matrixAddress",
                    "establishmentAddress",
                    "establishmentCode",
                    "emissionPointCode",
                    "series",
                    "invoiceSchemaVersion",
                    "invoiceSchemaVersionCode",
                    "obligatedToKeepAccounting",
                    "productionEnabled",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID),
            name = "organization_sri_settings_org_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("ruc", "environment"),
            name = "organization_sri_settings_ruc_environment_idx",
        )
    }
}
