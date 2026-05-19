package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

/**
 * Organization-level SRI configuration used by backend emission flows.
 *
 * This aggregate deliberately stores only operational tax identity/configuration.
 * Signature files and passwords are managed separately by the electronic signature module.
 */
data class OrganizationSriSettings(
    val organizationId: String,
    val environment: SriEnvironment,
    val ruc: String,
    val legalName: String,
    val commercialName: String?,
    val matrixAddress: String,
    val establishmentAddress: String,
    val establishmentCode: String,
    val emissionPointCode: String,
    val invoiceSchemaVersion: SriInvoiceSchemaVersion = SriInvoiceSchemaVersion.V2_1_0,
    val specialTaxpayerCode: String? = null,
    val obligatedToKeepAccounting: Boolean,
    val rimpeLegend: String? = null,
    val productionEnabled: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val updatedBy: String,
    val version: Long = 1,
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization SRI settings organization id cannot be blank.")
        if (!RUC_PATTERN.matches(ruc)) throw DomainRuleViolation("SRI settings RUC must contain exactly 13 digits.")
        if (legalName.isBlank()) throw DomainRuleViolation("SRI settings legal name cannot be blank.")
        if (matrixAddress.isBlank()) throw DomainRuleViolation("SRI settings matrix address cannot be blank.")
        if (establishmentAddress.isBlank()) throw DomainRuleViolation("SRI settings establishment address cannot be blank.")
        if (!SERIES_CODE_PATTERN.matches(establishmentCode)) {
            throw DomainRuleViolation("SRI establishment code must contain exactly 3 digits.")
        }
        if (!SERIES_CODE_PATTERN.matches(emissionPointCode)) {
            throw DomainRuleViolation("SRI emission point code must contain exactly 3 digits.")
        }
        if (updatedBy.isBlank()) throw DomainRuleViolation("SRI settings updatedBy cannot be blank.")
        if (version < 1) throw DomainRuleViolation("SRI settings version must be at least 1.")
        if (schemaVersion < 1) throw DomainRuleViolation("SRI settings schema version must be at least 1.")
        commercialName?.let { if (it.isBlank()) throw DomainRuleViolation("Commercial name cannot be blank when provided.") }
        specialTaxpayerCode?.let { if (it.isBlank()) throw DomainRuleViolation("Special taxpayer code cannot be blank when provided.") }
        rimpeLegend?.let { if (it.isBlank()) throw DomainRuleViolation("RIMPE legend cannot be blank when provided.") }
    }

    val series: SriSeries get() = SriSeries(establishmentCode = establishmentCode, emissionPointCode = emissionPointCode)

    fun update(
        environment: SriEnvironment,
        ruc: String,
        legalName: String,
        commercialName: String?,
        matrixAddress: String,
        establishmentAddress: String,
        establishmentCode: String,
        emissionPointCode: String,
        invoiceSchemaVersion: SriInvoiceSchemaVersion,
        specialTaxpayerCode: String?,
        obligatedToKeepAccounting: Boolean,
        rimpeLegend: String?,
        actorUserId: String,
        now: Instant,
    ): OrganizationSriSettings = copy(
        environment = environment,
        ruc = ruc.trim(),
        legalName = legalName.trim(),
        commercialName = commercialName.normalizedNullable(),
        matrixAddress = matrixAddress.trim(),
        establishmentAddress = establishmentAddress.trim(),
        establishmentCode = establishmentCode.trim(),
        emissionPointCode = emissionPointCode.trim(),
        invoiceSchemaVersion = invoiceSchemaVersion,
        specialTaxpayerCode = specialTaxpayerCode.normalizedNullable(),
        obligatedToKeepAccounting = obligatedToKeepAccounting,
        rimpeLegend = rimpeLegend.normalizedNullable(),
        updatedAt = now,
        updatedBy = actorUserId.trim(),
        version = version + 1,
    )

    fun enableProduction(actorUserId: String, now: Instant): OrganizationSriSettings = copy(
        productionEnabled = true,
        updatedAt = now,
        updatedBy = actorUserId.trim(),
        version = version + 1,
    )

    companion object {
        const val SCHEMA_VERSION: Int = 1
        private val RUC_PATTERN = Regex("^\\d{13}$")
        private val SERIES_CODE_PATTERN = Regex("^\\d{3}$")

        fun create(
            organizationId: String,
            environment: SriEnvironment,
            ruc: String,
            legalName: String,
            commercialName: String?,
            matrixAddress: String,
            establishmentAddress: String,
            establishmentCode: String,
            emissionPointCode: String,
            invoiceSchemaVersion: SriInvoiceSchemaVersion,
            specialTaxpayerCode: String?,
            obligatedToKeepAccounting: Boolean,
            rimpeLegend: String?,
            actorUserId: String,
            now: Instant,
        ): OrganizationSriSettings = OrganizationSriSettings(
            organizationId = organizationId.trim(),
            environment = environment,
            ruc = ruc.trim(),
            legalName = legalName.trim(),
            commercialName = commercialName.normalizedNullable(),
            matrixAddress = matrixAddress.trim(),
            establishmentAddress = establishmentAddress.trim(),
            establishmentCode = establishmentCode.trim(),
            emissionPointCode = emissionPointCode.trim(),
            invoiceSchemaVersion = invoiceSchemaVersion,
            specialTaxpayerCode = specialTaxpayerCode.normalizedNullable(),
            obligatedToKeepAccounting = obligatedToKeepAccounting,
            rimpeLegend = rimpeLegend.normalizedNullable(),
            productionEnabled = false,
            createdAt = now,
            updatedAt = now,
            updatedBy = actorUserId.trim(),
        )
    }
}

private fun String?.normalizedNullable(): String? = this?.trim()?.takeIf { it.isNotBlank() }
