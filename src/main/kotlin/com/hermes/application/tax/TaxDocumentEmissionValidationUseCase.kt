package com.hermes.application.tax

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxEmissionType
import com.hermes.domain.tax.TaxEmissionValidation
import com.hermes.domain.tax.TaxEngine
import com.hermes.domain.tax.TaxLineInput
import java.time.Clock
import java.time.Instant

class TaxDocumentEmissionValidationUseCase(
    private val profileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxDocumentEmissionValidationCommand): TaxDocumentEmissionValidationResult {
        assertCanValidateEmission(command.emissionType, command.actorEffectivePermissions)

        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (command.lines.isEmpty()) throw DomainRuleViolation("Document emission validation requires at least one line.")

        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")

        val forElectronicEmission = command.emissionType == TaxEmissionType.ELECTRONIC_INVOICE

        val inputs = command.lines.map { line ->
            val taxProfileCode = line.taxProfileCode.trim().lowercase()
            settings.assertCanUseProfile(taxProfileCode)

            val profile = profileRepository.findByCode(taxProfileCode)
                ?: throw DomainRuleViolation("Tax profile does not exist: $taxProfileCode.")

            TaxLineInput(
                lineId = line.lineId,
                description = line.description,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                discount = line.discount,
                taxProfileSnapshot = profile.snapshot(command.occurredAt, forEmission = forElectronicEmission),
                priceTaxMode = line.priceTaxMode,
            )
        }

        val calculation = TaxEngine.calculate(inputs)

        TaxEmissionValidation.assertCanPrepareEmission(
            emissionType = command.emissionType,
            lines = calculation.lines,
            summary = calculation.summary,
        )

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_DOCUMENT_EMISSION_VALIDATED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = null,
                after = mapOf(
                    "emissionType" to command.emissionType.name,
                    "lineCount" to calculation.lines.size.toString(),
                    "grandTotal" to calculation.summary.grandTotal.amount.toPlainString(),
                    "currency" to calculation.summary.currency.value,
                ),
                createdAt = Instant.now(clock),
            )
        )

        return TaxDocumentEmissionValidationResult(
            organizationId = organizationId,
            emissionType = command.emissionType,
            occurredAt = command.occurredAt,
            calculation = calculation,
        )
    }

    private fun assertCanValidateEmission(
        emissionType: TaxEmissionType,
        effectivePermissions: Set<String>,
    ) {
        val requiredPermission = when (emissionType) {
            TaxEmissionType.INTERNAL_TICKET -> PermissionCatalog.DOCUMENTS_GENERATE_INTERNAL_TICKET
            TaxEmissionType.PHYSICAL_SALE_NOTE_REGISTRY -> PermissionCatalog.DOCUMENTS_GENERATE_PHYSICAL_SALE_NOTE_REGISTRY
            TaxEmissionType.ELECTRONIC_INVOICE -> PermissionCatalog.DOCUMENTS_ISSUE_ELECTRONIC_INVOICE
        }

        val allowed = PermissionRules.canPerform(effectivePermissions, requiredPermission) ||
            PermissionRules.canPerform(effectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)

        if (!allowed) {
            throw DomainRuleViolation(
                "Missing any required permission: $requiredPermission, ${PermissionCatalog.TAX_SETTINGS_VIEW}."
            )
        }
    }
}
