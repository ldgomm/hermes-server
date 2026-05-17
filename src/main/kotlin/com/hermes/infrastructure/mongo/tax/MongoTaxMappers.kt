package com.hermes.infrastructure.mongo.tax

import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxKind
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxProfileStatus
import com.hermes.domain.tax.TaxRate
import com.hermes.domain.tax.TaxRateStatus
import com.hermes.domain.tax.TaxRegimeCode
import com.hermes.domain.tax.TaxSource
import com.hermes.domain.tax.TaxTreatment
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoDecimalMapper
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import org.bson.Document
import java.time.Instant

object MongoTaxMappers {
    fun taxRateToDocument(rate: TaxRate): Document =
        Document(MongoDocumentFields.ID, rate.id)
            .append("countryCode", "EC")
            .append("code", rate.code)
            .append("name", rate.name)
            .append("kind", rate.kind.name)
            .append("rate", MongoDecimalMapper.percentageToDecimal128(rate.rate))
            .append("status", rate.status.name)
            .append("sriTaxCode", rate.sriTaxCode)
            .append("sriRateCode", rate.sriRateCode)
            .append("legalBasis", rate.legalBasis)
            .append("effectiveFrom", MongoInstantMapper.toDate(rate.effectiveFrom))
            .append("effectiveTo", rate.effectiveTo?.let(MongoInstantMapper::toDate))
            .append("source", rate.source.name)
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(rate.createdAt))
            .append(MongoDocumentFields.CREATED_BY, null)
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(rate.updatedAt))
            .append(MongoDocumentFields.UPDATED_BY, null)
            .append(MongoDocumentFields.VERSION, rate.version.toInt())
            .append(MongoDocumentFields.SCHEMA_VERSION, rate.schemaVersion)

    fun taxRateFromDocument(document: Document): TaxRate =
        TaxRate(
            id = document.requiredString(MongoDocumentFields.ID),
            code = document.requiredString("code"),
            name = document.requiredString("name"),
            kind = document.requiredEnum("kind", TaxKind::valueOf),
            rate = MongoDecimalMapper.readRequired(document, "rate").setScale(TaxRate.RATE_SCALE),
            status = document.requiredEnum("status", TaxRateStatus::valueOf),
            sriTaxCode = document.optionalString("sriTaxCode"),
            sriRateCode = document.optionalString("sriRateCode"),
            legalBasis = document.requiredString("legalBasis"),
            effectiveFrom = MongoInstantMapper.readRequired(document, "effectiveFrom"),
            effectiveTo = MongoInstantMapper.readOptional(document, "effectiveTo"),
            source = document.optionalEnum("source", TaxSource.SYSTEM_SEED, TaxSource::valueOf),
            createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
            updatedAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.UPDATED_AT),
            version = document.optionalLong(MongoDocumentFields.VERSION, 1L),
            schemaVersion = document.optionalInt(MongoDocumentFields.SCHEMA_VERSION, 1),
        )

    fun taxProfileToDocument(profile: TaxProfile): Document =
        Document(MongoDocumentFields.ID, profile.id)
            .append("countryCode", "EC")
            .append("code", profile.code)
            .append("name", profile.name)
            .append("treatment", profile.treatment.name)
            .append("status", profile.status.name)
            .append("taxRate", profile.taxRate?.let(::taxRateToDocument))
            .append("sriTaxCode", profile.sriTaxCode)
            .append("sriRateCode", profile.sriRateCode)
            .append("legalBasis", profile.legalBasis)
            .append("effectiveFrom", MongoInstantMapper.toDate(profile.effectiveFrom))
            .append("effectiveTo", profile.effectiveTo?.let(MongoInstantMapper::toDate))
            .append("source", profile.source.name)
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(profile.createdAt))
            .append(MongoDocumentFields.CREATED_BY, null)
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(profile.updatedAt))
            .append(MongoDocumentFields.UPDATED_BY, null)
            .append(MongoDocumentFields.VERSION, profile.version.toInt())
            .append(MongoDocumentFields.SCHEMA_VERSION, profile.schemaVersion)

    fun taxProfileFromDocument(document: Document): TaxProfile =
        TaxProfile(
            id = document.requiredString(MongoDocumentFields.ID),
            code = document.requiredString("code"),
            name = document.requiredString("name"),
            treatment = document.requiredEnum("treatment", TaxTreatment::valueOf),
            status = document.requiredEnum("status", TaxProfileStatus::valueOf),
            taxRate = document.get("taxRate", Document::class.java)?.let(::taxRateFromDocument),
            sriTaxCode = document.optionalString("sriTaxCode"),
            sriRateCode = document.optionalString("sriRateCode"),
            legalBasis = document.requiredString("legalBasis"),
            effectiveFrom = MongoInstantMapper.readRequired(document, "effectiveFrom"),
            effectiveTo = MongoInstantMapper.readOptional(document, "effectiveTo"),
            source = document.optionalEnum("source", TaxSource.SYSTEM_SEED, TaxSource::valueOf),
            createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
            updatedAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.UPDATED_AT),
            version = document.optionalLong(MongoDocumentFields.VERSION, 1L),
            schemaVersion = document.optionalInt(MongoDocumentFields.SCHEMA_VERSION, 1),
        )

    fun organizationTaxSettingsToDocument(settings: OrganizationTaxSettings): Document =
        Document(MongoDocumentFields.ID, settings.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, settings.organizationId)
            .append("regime", settings.regime.name)
            .append("defaultTaxProfileCode", settings.defaultTaxProfileCode)
            .append("enabledTaxProfileCodes", settings.enabledTaxProfileCodes.sorted())
            .append("allowTaxInclusivePrices", settings.allowTaxInclusivePrices)
            .append("allowManualLineDiscounts", settings.allowManualLineDiscounts)
            .append("requireTaxProfileForCatalogItems", settings.requireTaxProfileForCatalogItems)
            .append("status", settings.status.name)
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(settings.createdAt))
            .append(MongoDocumentFields.CREATED_BY, settings.createdBy)
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(settings.updatedAt))
            .append(MongoDocumentFields.UPDATED_BY, settings.updatedBy)
            .append(MongoDocumentFields.VERSION, settings.version.toInt())
            .append(MongoDocumentFields.SCHEMA_VERSION, settings.schemaVersion)

    fun organizationTaxSettingsFromDocument(document: Document): OrganizationTaxSettings =
        OrganizationTaxSettings(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            regime = document.requiredEnum("regime", TaxRegimeCode::valueOf),
            defaultTaxProfileCode = document.requiredString("defaultTaxProfileCode"),
            enabledTaxProfileCodes = document.stringSet("enabledTaxProfileCodes"),
            allowTaxInclusivePrices = document.getBoolean("allowTaxInclusivePrices", false),
            allowManualLineDiscounts = document.getBoolean("allowManualLineDiscounts", false),
            requireTaxProfileForCatalogItems = document.getBoolean("requireTaxProfileForCatalogItems", true),
            status = document.requiredEnum("status", OrganizationTaxSettingsStatus::valueOf),
            createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
            updatedAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.UPDATED_AT),
            createdBy = document.requiredString(MongoDocumentFields.CREATED_BY),
            updatedBy = document.requiredString(MongoDocumentFields.UPDATED_BY),
            version = document.optionalLong(MongoDocumentFields.VERSION, 1L),
            schemaVersion = document.optionalInt(MongoDocumentFields.SCHEMA_VERSION, 1),
        )

    private fun Document.requiredString(field: String): String =
        getString(field)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Mongo tax document is missing required string field '$field'.")

    private fun Document.optionalString(field: String): String? =
        getString(field)?.takeIf { it.isNotBlank() }

    private fun Document.optionalInt(field: String, defaultValue: Int): Int =
        when (val raw = get(field)) {
            null -> defaultValue
            is Int -> raw
            is Long -> raw.toInt()
            is Number -> raw.toInt()
            else -> error("Mongo field '$field' must be numeric.")
        }

    private fun Document.optionalLong(field: String, defaultValue: Long): Long =
        when (val raw = get(field)) {
            null -> defaultValue
            is Int -> raw.toLong()
            is Long -> raw
            is Number -> raw.toLong()
            else -> error("Mongo field '$field' must be numeric.")
        }

    private fun <T : Enum<T>> Document.requiredEnum(field: String, mapper: (String) -> T): T =
        runCatching { mapper(requiredString(field)) }.getOrElse { error ->
            throw IllegalArgumentException("Mongo field '$field' contains invalid enum value '${getString(field)}'.", error)
        }

    private fun <T : Enum<T>> Document.optionalEnum(field: String, defaultValue: T, mapper: (String) -> T): T {
        val raw = getString(field) ?: return defaultValue
        return runCatching { mapper(raw) }.getOrElse { error ->
            throw IllegalArgumentException("Mongo field '$field' contains invalid enum value '$raw'.", error)
        }
    }

    private fun Document.stringSet(field: String): Set<String> =
        when (val raw = get(field)) {
            is List<*> -> raw.filterIsInstance<String>().map { it.trim() }.filter { it.isNotBlank() }.toSet()
            else -> emptySet()
        }
}
