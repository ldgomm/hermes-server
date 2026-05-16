package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes

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
            .append("taxName", MongoMigrationSupport.enum(listOf("IVA", "ICE", "IRBPNR", "NO_TAX")))
            .append("ratePercent", MongoMigrationSupport.decimal())
            .append("sriTaxCode", MongoMigrationSupport.string(maxLength = 16))
            .append("sriRateCode", MongoMigrationSupport.string(maxLength = 16))
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "deprecated", "archived")))
            .append("effectiveFrom", MongoMigrationSupport.date())
            .append("effectiveTo", MongoMigrationSupport.nullableDate())
            .append("legalReference", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append(
                "source",
                MongoMigrationSupport.enum(
                    listOf(
                        "sri_verified",
                        "admin_tax_configuration",
                        "migration_seed",
                        "unknown"
                    )
                )
            )

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.TAX_RATES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "countryCode",
                    "taxName",
                    "ratePercent",
                    "sriTaxCode",
                    "sriRateCode",
                    "status",
                    "effectiveFrom",
                    "source",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("countryCode", "taxName", "status", "effectiveFrom"),
            name = "tax_rates_country_tax_status_effective_from_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("countryCode", "sriTaxCode", "sriRateCode", "effectiveFrom"),
            name = "tax_rates_country_sri_codes_effective_unique_idx",
            unique = true,
        )
    }

    private fun createTaxProfiles(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("organizationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("code", MongoMigrationSupport.string(maxLength = 128))
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append("taxName", MongoMigrationSupport.enum(listOf("IVA", "ICE", "IRBPNR", "NO_TAX")))
            .append("taxRateId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("ratePercent", MongoMigrationSupport.decimal())
            .append("sriTaxCode", MongoMigrationSupport.string(maxLength = 16))
            .append("sriRateCode", MongoMigrationSupport.string(maxLength = 16))
            .append(
                "profileType",
                MongoMigrationSupport.enum(
                    listOf(
                        "iva_current_full",
                        "iva_reduced_or_special",
                        "iva_0",
                        "exempt_iva",
                        "not_subject_to_iva",
                        "no_tax_internal"
                    )
                )
            )
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "deprecated", "archived")))
            .append("effectiveFrom", MongoMigrationSupport.date())
            .append("effectiveTo", MongoMigrationSupport.nullableDate())
            .append(
                "snapshotPolicy",
                MongoMigrationSupport.enum(listOf("required_on_sale", "required_on_document", "internal_only"))
            )

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.TAX_PROFILES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "code",
                    "name",
                    "taxName",
                    "ratePercent",
                    "sriTaxCode",
                    "sriRateCode",
                    "profileType",
                    "status",
                    "effectiveFrom",
                    "snapshotPolicy",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("code"),
            name = "tax_profiles_global_code_unique_idx",
            unique = true,
            partialFilterExpression = Filters.exists("organizationId", FalseBoolean.FALSE),
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "code"),
            name = "tax_profiles_org_code_unique_idx",
            unique = true,
            partialFilterExpression = Filters.exists("organizationId", true),
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("profileType", "status"),
            name = "tax_profiles_type_status_idx",
        )
    }

    private fun createOrganizationTaxSettings(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append(
                "taxRegime",
                MongoMigrationSupport.enum(
                    listOf(
                        "rimpe_popular",
                        "rimpe_entrepreneur",
                        "general",
                        "unknown",
                        "custom_verified"
                    )
                )
            )
            .append("taxMode", MongoMigrationSupport.enum(listOf("no_iva", "iva", "mixed_by_item", "custom_verified")))
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "replaced", "archived")))
            .append("activeFrom", MongoMigrationSupport.date())
            .append("activeTo", MongoMigrationSupport.nullableDate())
            .append("defaultTaxProfileId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("requiresAccounting", MongoMigrationSupport.bool())
            .append(
                "source",
                MongoMigrationSupport.enum(listOf("onboarding", "admin_tax_configuration", "sri_verified", "migration"))
            )

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.ORGANIZATION_TAX_SETTINGS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "taxRegime",
                    "taxMode",
                    "status",
                    "activeFrom",
                    "requiresAccounting",
                    "source",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "status"),
            name = "organization_tax_settings_org_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId"),
            name = "organization_tax_settings_org_active_unique_idx",
            unique = true,
            partialFilterExpression = Filters.eq("status", "active"),
        )
    }
}

private object FalseBoolean {
    const val FALSE: Boolean = false
}
