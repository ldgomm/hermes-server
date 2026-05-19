package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class UpdateAdminBusinessUseCase(
    private val readRepository: AdminBusinessRepository,
    private val mutationRepository: AdminBusinessMutationRepository,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UpdateAdminBusinessCommand): AdminBusinessResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.ORGANIZATION_UPDATE)

        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Business update reason")
        val now = Instant.now(clock)

        val current = readRepository.findBusiness(organizationId)
            ?: throw DomainRuleViolation("Organization does not exist.")

        val normalizedCountryCode = command.countryCode?.normalizeCountryCode()
        val normalizedTaxId = command.taxId?.required("Organization tax id")
        val normalizedLegalName = command.legalName?.required("Organization legal name")
        val normalizedCommercialName = command.commercialName?.required("Organization commercial name")
        val normalizedCurrency = command.defaultCurrency?.normalizeCurrencyCode()
        val normalizedTimezone = command.timezone?.required("Organization timezone")

        val targetCountryCode = normalizedCountryCode ?: current.countryCode
        val targetTaxId = normalizedTaxId ?: current.taxId

        if ((normalizedCountryCode != null || normalizedTaxId != null) &&
            mutationRepository.existsBusinessWithTaxId(
                countryCode = targetCountryCode,
                taxId = targetTaxId,
                excludeOrganizationId = organizationId,
            )
        ) {
            throw DomainRuleViolation("Another organization already uses this country code and tax id.")
        }

        val patch = AdminBusinessUpdatePatch(
            organizationId = organizationId,
            countryCode = normalizedCountryCode.takeIfChanged(current.countryCode),
            taxId = normalizedTaxId.takeIfChanged(current.taxId),
            legalName = normalizedLegalName.takeIfChanged(current.legalName),
            commercialName = normalizedCommercialName.takeIfChanged(current.commercialName),
            defaultCurrency = normalizedCurrency.takeIfChanged(current.defaultCurrency),
            timezone = normalizedTimezone.takeIfChanged(current.timezone),
            updatedBy = actorUserId,
            updatedAt = now,
        )

        if (!patch.hasChanges()) {
            throw DomainRuleViolation("Business update does not contain changes.")
        }

        val updated = mutationRepository.updateBusiness(patch)

        auditLogger.log(
            AdminBusinessAuditEvent(
                action = AdminBusinessAuditAction.BUSINESS_SETTINGS_UPDATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = organizationId,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessResult(updated)
    }

    private fun AdminBusinessUpdatePatch.hasChanges(): Boolean = listOf(
        countryCode,
        taxId,
        legalName,
        commercialName,
        defaultCurrency,
        timezone,
    ).any { it != null }

    private fun String.normalizeCountryCode(): String {
        val value = required("Organization country code").uppercase()
        if (value.length != 2) throw DomainRuleViolation("Organization country code must use ISO alpha-2 format.")
        return value
    }

    private fun String.normalizeCurrencyCode(): String {
        val value = required("Organization default currency").uppercase()
        if (value.length != 3) throw DomainRuleViolation("Organization default currency must use ISO alpha-3 format.")
        return value
    }

    private fun String?.takeIfChanged(current: String?): String? = when {
        this == null -> null
        this == current -> null
        else -> this
    }

    private fun AdminBusinessProfile.toAuditMap(): Map<String, String?> = mapOf(
        "id" to id,
        "countryCode" to countryCode,
        "taxId" to taxId,
        "legalName" to legalName,
        "commercialName" to commercialName,
        "defaultCurrency" to defaultCurrency,
        "timezone" to timezone,
        "status" to status,
        "version" to version.toString(),
    )
}
