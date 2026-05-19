package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.*
import com.hermes.domain.electronicinvoicing.OrganizationSriSettings
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriInvoiceSchemaVersion
import kotlinx.serialization.Serializable

@Serializable
data class SriSettingsRequest(
    val environment: String = "test",
    val ruc: String,
    val legalName: String,
    val commercialName: String? = null,
    val matrixAddress: String,
    val establishmentAddress: String,
    val establishmentCode: String,
    val emissionPointCode: String,
    val invoiceSchemaVersion: String = "2.1.0",
    val specialTaxpayerCode: String? = null,
    val obligatedToKeepAccounting: Boolean = false,
    val rimpeLegend: String? = null,
)

@Serializable
data class SriSettingsEnvelopeResponse(
    val settings: SriSettingsResponse?,
)

@Serializable
data class SriSettingsResponse(
    val organizationId: String,
    val environment: String,
    val ruc: String,
    val legalName: String,
    val commercialName: String?,
    val matrixAddress: String,
    val establishmentAddress: String,
    val establishmentCode: String,
    val emissionPointCode: String,
    val series: String,
    val invoiceSchemaVersion: String,
    val invoiceSchemaVersionCode: String,
    val specialTaxpayerCode: String?,
    val obligatedToKeepAccounting: Boolean,
    val rimpeLegend: String?,
    val productionEnabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val updatedBy: String,
    val version: Long,
)

@Serializable
data class SriReadinessResponse(
    val organizationId: String,
    val ready: Boolean,
    val environment: String?,
    val productionEnabled: Boolean,
    val checkedAt: String,
    val checks: List<SriReadinessCheckResponse>,
)

@Serializable
data class SriReadinessCheckResponse(
    val code: String,
    val ok: Boolean,
    val severity: String,
    val message: String,
)

fun SriSettingsRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): UpsertOrganizationSriSettingsCommand = UpsertOrganizationSriSettingsCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    environment = SriEnvironment.fromStorage(environment),
    ruc = ruc,
    legalName = legalName,
    commercialName = commercialName,
    matrixAddress = matrixAddress,
    establishmentAddress = establishmentAddress,
    establishmentCode = establishmentCode,
    emissionPointCode = emissionPointCode,
    invoiceSchemaVersion = SriInvoiceSchemaVersion.fromVersion(invoiceSchemaVersion),
    specialTaxpayerCode = specialTaxpayerCode,
    obligatedToKeepAccounting = obligatedToKeepAccounting,
    rimpeLegend = rimpeLegend,
)

fun readinessCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): CheckOrganizationSriReadinessCommand = CheckOrganizationSriReadinessCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
)

fun OrganizationSriSettingsResult.toResponse(): SriSettingsEnvelopeResponse =
    SriSettingsEnvelopeResponse(settings = settings?.toResponse())

fun OrganizationSriSettings.toResponse(): SriSettingsResponse = SriSettingsResponse(
    organizationId = organizationId,
    environment = environment.storageValue,
    ruc = ruc,
    legalName = legalName,
    commercialName = commercialName,
    matrixAddress = matrixAddress,
    establishmentAddress = establishmentAddress,
    establishmentCode = establishmentCode,
    emissionPointCode = emissionPointCode,
    series = series.displayValue,
    invoiceSchemaVersion = invoiceSchemaVersion.version,
    invoiceSchemaVersionCode = invoiceSchemaVersion.schemaVersionCode,
    specialTaxpayerCode = specialTaxpayerCode,
    obligatedToKeepAccounting = obligatedToKeepAccounting,
    rimpeLegend = rimpeLegend,
    productionEnabled = productionEnabled,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    updatedBy = updatedBy,
    version = version,
)

fun SriReadinessResult.toResponse(): SriReadinessResponse = SriReadinessResponse(
    organizationId = organizationId,
    ready = ready,
    environment = environment?.storageValue,
    productionEnabled = productionEnabled,
    checkedAt = checkedAt.toString(),
    checks = checks.map { it.toResponse() },
)

private fun SriReadinessCheck.toResponse(): SriReadinessCheckResponse = SriReadinessCheckResponse(
    code = code,
    ok = ok,
    severity = severity.name,
    message = message,
)
