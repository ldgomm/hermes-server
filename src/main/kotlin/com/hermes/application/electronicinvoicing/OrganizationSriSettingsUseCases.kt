package com.hermes.application.electronicinvoicing

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.signature.ElectronicSignatureStatus
import java.time.Clock
import java.time.Instant

class GetOrganizationSriSettingsUseCase(
    private val repository: OrganizationSriSettingsRepository,
) {
    fun execute(command: GetOrganizationSriSettingsCommand): OrganizationSriSettingsResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS
        )
        val settings = repository.findByOrganizationId(command.organizationId.trim())
        return OrganizationSriSettingsResult(settings)
    }
}

class UpsertOrganizationSriSettingsUseCase(
    private val repository: OrganizationSriSettingsRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UpsertOrganizationSriSettingsCommand): OrganizationSriSettingsResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS
        )
        val now = Instant.now(clock)
        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        val actorUserId = command.actorUserId.trim()
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required.")

        val existing = repository.findByOrganizationId(organizationId)
        val settings = if (existing == null) {
            OrganizationSriSettings.create(
                organizationId = organizationId,
                environment = command.environment,
                ruc = command.ruc,
                legalName = command.legalName,
                commercialName = command.commercialName,
                matrixAddress = command.matrixAddress,
                establishmentAddress = command.establishmentAddress,
                establishmentCode = command.establishmentCode,
                emissionPointCode = command.emissionPointCode,
                invoiceSchemaVersion = command.invoiceSchemaVersion,
                specialTaxpayerCode = command.specialTaxpayerCode,
                obligatedToKeepAccounting = command.obligatedToKeepAccounting,
                rimpeLegend = command.rimpeLegend,
                actorUserId = actorUserId,
                now = now,
            )
        } else {
            existing.update(
                environment = command.environment,
                ruc = command.ruc,
                legalName = command.legalName,
                commercialName = command.commercialName,
                matrixAddress = command.matrixAddress,
                establishmentAddress = command.establishmentAddress,
                establishmentCode = command.establishmentCode,
                emissionPointCode = command.emissionPointCode,
                invoiceSchemaVersion = command.invoiceSchemaVersion,
                specialTaxpayerCode = command.specialTaxpayerCode,
                obligatedToKeepAccounting = command.obligatedToKeepAccounting,
                rimpeLegend = command.rimpeLegend,
                actorUserId = actorUserId,
                now = now,
            )
        }
        return OrganizationSriSettingsResult(repository.save(settings))
    }
}

class CheckOrganizationSriReadinessUseCase(
    private val settingsRepository: OrganizationSriSettingsRepository,
    private val signatureRepository: ElectronicSignatureRepository,
    private val sequenceRepository: ElectronicSequenceRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CheckOrganizationSriReadinessCommand): SriReadinessResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS
        )
        val now = Instant.now(clock)
        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")

        val checks = mutableListOf<SriReadinessCheck>()
        val settings = settingsRepository.findByOrganizationId(organizationId)

        checks += SriReadinessCheck(
            code = "sri_settings_configured",
            ok = settings != null,
            severity = SriReadinessSeverity.BLOCKER,
            message = if (settings == null) "SRI settings are not configured." else "SRI settings exist.",
        )

        if (settings != null) {
            checks += SriReadinessCheck(
                code = "sri_ruc_valid",
                ok = settings.ruc.matches(Regex("^\\d{13}$")),
                severity = SriReadinessSeverity.BLOCKER,
                message = "RUC must contain exactly 13 digits.",
            )
            checks += SriReadinessCheck(
                code = "sri_emitter_identity_complete",
                ok = settings.legalName.isNotBlank() && settings.matrixAddress.isNotBlank() && settings.establishmentAddress.isNotBlank(),
                severity = SriReadinessSeverity.BLOCKER,
                message = "Emitter legal name and addresses must be complete.",
            )
            checks += SriReadinessCheck(
                code = "sri_series_configured",
                ok = settings.establishmentCode.matches(Regex("^\\d{3}$")) && settings.emissionPointCode.matches(Regex("^\\d{3}$")),
                severity = SriReadinessSeverity.BLOCKER,
                message = "Establishment and emission point codes must contain 3 digits each.",
            )

            val activeSignature = signatureRepository.findActiveByOrganizationId(organizationId)
            val effectiveStatus = activeSignature?.effectiveStatus(now)
            checks += SriReadinessCheck(
                code = "electronic_signature_active",
                ok = activeSignature != null && effectiveStatus == ElectronicSignatureStatus.VALID,
                severity = SriReadinessSeverity.BLOCKER,
                message = when {
                    activeSignature == null -> "No active electronic signature exists for this organization."
                    effectiveStatus != ElectronicSignatureStatus.VALID -> "Active electronic signature is not currently valid: $effectiveStatus."
                    else -> "An active and valid electronic signature exists."
                },
            )

            val sequence = sequenceRepository.findByKey(
                ElectronicSequenceKey(
                    organizationId = organizationId,
                    environment = settings.environment,
                    documentType = SriDocumentType.INVOICE,
                    series = settings.series,
                )
            )
            checks += SriReadinessCheck(
                code = "invoice_sequence_configured",
                ok = sequence != null && sequence.isActive,
                severity = SriReadinessSeverity.BLOCKER,
                message = if (sequence == null) "No active invoice sequence exists for the configured series." else "Invoice sequence exists.",
            )

            if (settings.environment == SriEnvironment.PRODUCTION) {
                checks += SriReadinessCheck(
                    code = "production_gate_enabled",
                    ok = settings.productionEnabled,
                    severity = SriReadinessSeverity.BLOCKER,
                    message = if (settings.productionEnabled) {
                        "Production has been enabled for this organization."
                    } else {
                        "Production is blocked until homologation and production gate are completed."
                    },
                )
            }
        }

        val ready = checks.all { it.ok || it.severity != SriReadinessSeverity.BLOCKER }
        return SriReadinessResult(
            organizationId = organizationId,
            ready = ready,
            environment = settings?.environment,
            productionEnabled = settings?.productionEnabled ?: false,
            checks = checks,
            checkedAt = now,
        )
    }
}

data class GetOrganizationSriSettingsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class UpsertOrganizationSriSettingsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
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
)

data class CheckOrganizationSriReadinessCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class OrganizationSriSettingsResult(
    val settings: OrganizationSriSettings?,
)

data class SriReadinessResult(
    val organizationId: String,
    val ready: Boolean,
    val environment: SriEnvironment?,
    val productionEnabled: Boolean,
    val checks: List<SriReadinessCheck>,
    val checkedAt: Instant,
)

data class SriReadinessCheck(
    val code: String,
    val ok: Boolean,
    val severity: SriReadinessSeverity,
    val message: String,
)

enum class SriReadinessSeverity {
    INFO,
    WARNING,
    BLOCKER,
}
