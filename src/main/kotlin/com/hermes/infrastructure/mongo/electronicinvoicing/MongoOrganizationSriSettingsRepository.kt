package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.OrganizationSriSettingsRepository
import com.hermes.domain.electronicinvoicing.OrganizationSriSettings
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriInvoiceSchemaVersion
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import java.time.Instant
import java.util.*

class MongoOrganizationSriSettingsRepository(
    database: MongoDatabase,
) : OrganizationSriSettingsRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(ElectronicInvoicingMongoCollectionNames.ORGANIZATION_SRI_SETTINGS)

    override fun findByOrganizationId(organizationId: String): OrganizationSriSettings? =
        collection.find(eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim())).firstOrNull()?.toDomain()

    override fun save(settings: OrganizationSriSettings): OrganizationSriSettings {
        collection.replaceOne(
            eq(MongoDocumentFields.ORGANIZATION_ID, settings.organizationId),
            settings.toDocument(),
            ReplaceOptions().upsert(true),
        )
        return findByOrganizationId(settings.organizationId)
            ?: throw IllegalStateException("Organization SRI settings were not saved.")
    }
}

private fun OrganizationSriSettings.toDocument(): Document = Document().append(MongoDocumentFields.ID, organizationId)
    .append(MongoDocumentFields.ORGANIZATION_ID, organizationId).append("environment", environment.storageValue)
    .append("ruc", ruc).append("legalName", legalName).append("commercialName", commercialName)
    .append("matrixAddress", matrixAddress).append("establishmentAddress", establishmentAddress)
    .append("establishmentCode", establishmentCode).append("emissionPointCode", emissionPointCode)
    .append("series", series.value).append("invoiceSchemaVersion", invoiceSchemaVersion.version)
    .append("invoiceSchemaVersionCode", invoiceSchemaVersion.schemaVersionCode)
    .append("specialTaxpayerCode", specialTaxpayerCode).append("obligatedToKeepAccounting", obligatedToKeepAccounting)
    .append("rimpeLegend", rimpeLegend).append("productionEnabled", productionEnabled)
    .append(MongoDocumentFields.CREATED_AT, Date.from(createdAt)).append(MongoDocumentFields.CREATED_BY, updatedBy)
    .append(MongoDocumentFields.UPDATED_AT, Date.from(updatedAt)).append(MongoDocumentFields.UPDATED_BY, updatedBy)
    .append(MongoDocumentFields.VERSION, version).append(MongoDocumentFields.SCHEMA_VERSION, schemaVersion)

private fun Document.toDomain(): OrganizationSriSettings = OrganizationSriSettings(
    organizationId = getString(MongoDocumentFields.ORGANIZATION_ID).required(MongoDocumentFields.ORGANIZATION_ID),
    environment = SriEnvironment.fromStorage(getString("environment").required("environment")),
    ruc = getString("ruc").required("ruc"),
    legalName = getString("legalName").required("legalName"),
    commercialName = getString("commercialName"),
    matrixAddress = getString("matrixAddress").required("matrixAddress"),
    establishmentAddress = getString("establishmentAddress").required("establishmentAddress"),
    establishmentCode = getString("establishmentCode").required("establishmentCode"),
    emissionPointCode = getString("emissionPointCode").required("emissionPointCode"),
    invoiceSchemaVersion = getString("invoiceSchemaVersion")?.let(SriInvoiceSchemaVersion::fromVersion)
        ?: SriInvoiceSchemaVersion.V2_1_0,
    specialTaxpayerCode = getString("specialTaxpayerCode"),
    obligatedToKeepAccounting = getBoolean("obligatedToKeepAccounting", false),
    rimpeLegend = getString("rimpeLegend"),
    productionEnabled = getBoolean("productionEnabled", false),
    createdAt = readInstant(MongoDocumentFields.CREATED_AT),
    updatedAt = readInstant(MongoDocumentFields.UPDATED_AT),
    updatedBy = getString(MongoDocumentFields.UPDATED_BY) ?: getString(MongoDocumentFields.CREATED_BY) ?: "system",
    version = readLong(MongoDocumentFields.VERSION, 1L),
    schemaVersion = readInt(MongoDocumentFields.SCHEMA_VERSION, OrganizationSriSettings.SCHEMA_VERSION),
)

private fun Document.readInstant(field: String): Instant = MongoInstantMapper.readRequired(this, field)

private fun Document.readInt(field: String, default: Int): Int = when (val raw = this[field]) {
    null -> default
    is Int -> raw
    is Long -> raw.toInt()
    is Number -> raw.toInt()
    is String -> raw.toIntOrNull() ?: default
    else -> default
}

private fun Document.readLong(field: String, default: Long): Long = when (val raw = this[field]) {
    null -> default
    is Long -> raw
    is Int -> raw.toLong()
    is Number -> raw.toLong()
    is String -> raw.toLongOrNull() ?: default
    else -> default
}

private fun String?.required(field: String): String = this?.trim()?.takeIf { it.isNotBlank() }
    ?: throw IllegalArgumentException("Required field '$field' is missing or blank.")
