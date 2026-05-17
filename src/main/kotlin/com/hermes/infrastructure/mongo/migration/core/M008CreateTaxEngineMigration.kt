package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import org.bson.Document

object M008CreateTaxEngineMigration : MongoMigration {
    override val id: String = "M008_create_tax_engine"
    override val description: String = "Create tax rates, tax profiles and organization tax settings."

    override fun up(database: MongoDatabase) {
        createTaxRates(database)
        createTaxProfiles(database)
        createOrganizationTaxSettings(database)
    }

    private fun createTaxRates(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("countryCode", MongoMigrationSupport.enum(listOf("EC")))
            .append("code", MongoMigrationSupport.string(maxLength = 128))
            .append("name", MongoMigrationSupport.string(maxLength = 256))
            .append("kind", MongoMigrationSupport.enum(TAX_KIND_VALUES))
            .append("rate", MongoMigrationSupport.decimal())
            .append("status", MongoMigrationSupport.enum(TAX_RATE_STATUS_VALUES))
            .append("sriTaxCode", MongoMigrationSupport.nullableString(maxLength = 16))
            .append("sriRateCode", MongoMigrationSupport.nullableString(maxLength = 16))
            .append("legalBasis", MongoMigrationSupport.string(maxLength = 2048))
            .append("effectiveFrom", MongoMigrationSupport.date())
            .append("effectiveTo", MongoMigrationSupport.nullableDate())
            .append("source", MongoMigrationSupport.enum(TAX_SOURCE_VALUES))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.TAX_RATES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "countryCode",
                    "code",
                    "name",
                    "kind",
                    "rate",
                    "status",
                    "legalBasis",
                    "effectiveFrom",
                    "source",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("code"),
            name = "tax_rates_code_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("countryCode", "kind", "status", "effectiveFrom"),
            name = "tax_rates_country_kind_status_effective_from_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("countryCode", "sriTaxCode", "sriRateCode", "effectiveFrom"),
            name = "tax_rates_country_sri_codes_effective_idx",
            sparse = true,
        )
    }

    private fun createTaxProfiles(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("countryCode", MongoMigrationSupport.enum(listOf("EC")))
            .append("code", MongoMigrationSupport.string(maxLength = 128))
            .append("name", MongoMigrationSupport.string(maxLength = 256))
            .append("treatment", MongoMigrationSupport.enum(TAX_TREATMENT_VALUES))
            .append("status", MongoMigrationSupport.enum(TAX_PROFILE_STATUS_VALUES))
            .append("taxRate", nullableTaxRateObject())
            .append("sriTaxCode", MongoMigrationSupport.nullableString(maxLength = 16))
            .append("sriRateCode", MongoMigrationSupport.nullableString(maxLength = 16))
            .append("legalBasis", MongoMigrationSupport.string(maxLength = 2048))
            .append("effectiveFrom", MongoMigrationSupport.date())
            .append("effectiveTo", MongoMigrationSupport.nullableDate())
            .append("source", MongoMigrationSupport.enum(TAX_SOURCE_VALUES))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.TAX_PROFILES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "countryCode",
                    "code",
                    "name",
                    "treatment",
                    "status",
                    "legalBasis",
                    "effectiveFrom",
                    "source",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("code"),
            name = "tax_profiles_code_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("status", "treatment", "effectiveFrom"),
            name = "tax_profiles_status_treatment_effective_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("taxRate.code"),
            name = "tax_profiles_tax_rate_code_idx",
            sparse = true,
        )
    }

    private fun createOrganizationTaxSettings(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("regime", MongoMigrationSupport.enum(TAX_REGIME_VALUES))
            .append("defaultTaxProfileCode", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "enabledTaxProfileCodes",
                MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)),
            )
            .append("allowTaxInclusivePrices", MongoMigrationSupport.bool())
            .append("allowManualLineDiscounts", MongoMigrationSupport.bool())
            .append("requireTaxProfileForCatalogItems", MongoMigrationSupport.bool())
            .append("status", MongoMigrationSupport.enum(ORGANIZATION_TAX_SETTINGS_STATUS_VALUES))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.ORGANIZATION_TAX_SETTINGS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "regime",
                    "defaultTaxProfileCode",
                    "enabledTaxProfileCodes",
                    "allowTaxInclusivePrices",
                    "allowManualLineDiscounts",
                    "requireTaxProfileForCatalogItems",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId"),
            name = "organization_tax_settings_org_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "status"),
            name = "organization_tax_settings_org_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("enabledTaxProfileCodes"),
            name = "organization_tax_settings_enabled_profiles_idx",
        )
    }

    private fun nullableTaxRateObject(): Document =
        Document("bsonType", listOf("object", "null"))
            .append(
                "properties",
                Document()
                    .append("_id", MongoMigrationSupport.id(prefix = "taxr_"))
                    .append("countryCode", MongoMigrationSupport.enum(listOf("EC")))
                    .append("code", MongoMigrationSupport.string(maxLength = 128))
                    .append("name", MongoMigrationSupport.string(maxLength = 256))
                    .append("kind", MongoMigrationSupport.enum(TAX_KIND_VALUES))
                    .append("rate", MongoMigrationSupport.decimal())
                    .append("status", MongoMigrationSupport.enum(TAX_RATE_STATUS_VALUES))
                    .append("sriTaxCode", MongoMigrationSupport.nullableString(maxLength = 16))
                    .append("sriRateCode", MongoMigrationSupport.nullableString(maxLength = 16))
                    .append("legalBasis", MongoMigrationSupport.string(maxLength = 2048))
                    .append("effectiveFrom", MongoMigrationSupport.date())
                    .append("effectiveTo", MongoMigrationSupport.nullableDate())
                    .append("source", MongoMigrationSupport.enum(TAX_SOURCE_VALUES))
                    .append("createdAt", MongoMigrationSupport.date())
                    .append("createdBy", MongoMigrationSupport.nullableString(maxLength = 128))
                    .append("updatedAt", MongoMigrationSupport.date())
                    .append("updatedBy", MongoMigrationSupport.nullableString(maxLength = 128))
                    .append("version", MongoMigrationSupport.int())
                    .append("schemaVersion", MongoMigrationSupport.int()),
            )

    private val TAX_KIND_VALUES = listOf("IVA", "ICE", "IRBPNR", "ISD", "OTHER")
    private val TAX_TREATMENT_VALUES = listOf(
        "IVA_FULL",
        "IVA_REDUCED_OR_SPECIAL",
        "IVA_ZERO",
        "EXEMPT_IVA",
        "NOT_SUBJECT_TO_IVA",
        "NO_TAX_INTERNAL",
    )
    private val TAX_RATE_STATUS_VALUES = listOf("DRAFT", "ACTIVE", "DEPRECATED", "ARCHIVED")
    private val TAX_PROFILE_STATUS_VALUES = listOf("DRAFT", "ACTIVE", "DEPRECATED", "ARCHIVED")
    private val TAX_SOURCE_VALUES = listOf("SYSTEM_SEED", "PLATFORM_ADMIN", "ORGANIZATION_ADMIN", "MIGRATION", "IMPORT")
    private val TAX_REGIME_VALUES = listOf("RIMPE_POPULAR", "RIMPE_ENTREPRENEUR", "GENERAL", "UNKNOWN", "CUSTOM_VERIFIED")
    private val ORGANIZATION_TAX_SETTINGS_STATUS_VALUES = listOf("ACTIVE", "SUSPENDED", "ARCHIVED")
}
