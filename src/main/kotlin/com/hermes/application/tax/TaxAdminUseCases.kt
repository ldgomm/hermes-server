package com.hermes.application.tax

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant

class TaxGetRateUseCase(
    private val rateRepository: TaxRateRepository,
) {
    fun execute(command: TaxGetRateCommand): TaxRateResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)

        val rate = rateRepository.findById(command.taxRateId.trim())
            ?: throw DomainRuleViolation("Tax rate does not exist.")

        return TaxRateResult(rate)
    }
}

class TaxCreateRateUseCase(
    private val rateRepository: TaxRateRepository,
    private val idGenerator: TaxIdGenerator,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxCreateRateCommand): TaxRateResult {
        assertCanManageTax(command.actorEffectivePermissions)

        val now = Instant.now(clock)
        val code = command.code.normalizedTaxCode("Tax rate code")

        if (rateRepository.findByCode(code) != null) {
            throw DomainRuleViolation("Tax rate code already exists: $code.")
        }

        val rate = TaxRate.of(
            id = idGenerator.newId("taxr"),
            code = code,
            name = command.name.requiredTrimmed("Tax rate name"),
            kind = command.kind,
            rate = command.rate.normalizedTaxRateString(),
            status = command.status,
            sriTaxCode = command.sriTaxCode.normalizedNullable(),
            sriRateCode = command.sriRateCode.normalizedNullable(),
            legalBasis = command.legalBasis.requiredTrimmed("Tax rate legal basis"),
            effectiveFrom = command.effectiveFrom,
            effectiveTo = command.effectiveTo,
            source = TaxSource.PLATFORM_ADMIN,
            now = now,
        )

        rateRepository.create(rate)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_RATE_CREATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = rate.id,
                before = emptyMap(),
                after = rate.toAuditMap(),
                reason = command.reason.normalizedNullable(),
                createdAt = now,
            )
        )

        return TaxRateResult(rate)
    }
}

class TaxUpdateRateUseCase(
    private val rateRepository: TaxRateRepository,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxUpdateRateCommand): TaxRateResult {
        assertCanManageTax(command.actorEffectivePermissions)

        val now = Instant.now(clock)
        val reason = command.reason.requiredTrimmed("Tax rate update reason")

        val current = rateRepository.findById(command.taxRateId.trim())
            ?: throw DomainRuleViolation("Tax rate does not exist.")

        val updated = current.copy(
            name = command.name?.requiredTrimmed("Tax rate name") ?: current.name,
            kind = command.kind ?: current.kind,
            rate = command.rate?.toTaxRateDecimal() ?: current.rate,
            status = command.status ?: current.status,
            sriTaxCode = when {
                command.clearSriTaxCode -> null
                command.sriTaxCode != null -> command.sriTaxCode.normalizedNullable()
                else -> current.sriTaxCode
            },
            sriRateCode = when {
                command.clearSriRateCode -> null
                command.sriRateCode != null -> command.sriRateCode.normalizedNullable()
                else -> current.sriRateCode
            },
            legalBasis = command.legalBasis?.requiredTrimmed("Tax rate legal basis") ?: current.legalBasis,
            effectiveFrom = command.effectiveFrom ?: current.effectiveFrom,
            effectiveTo = when {
                command.clearEffectiveTo -> null
                command.effectiveTo != null -> command.effectiveTo
                else -> current.effectiveTo
            },
            updatedAt = now,
            version = current.version + 1,
        )

        rateRepository.update(updated)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_RATE_UPDATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = updated.id,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return TaxRateResult(updated)
    }
}

class TaxGetProfileUseCase(
    private val profileRepository: TaxProfileRepository,
) {
    fun execute(command: TaxGetProfileCommand): TaxProfileResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)

        val profile = profileRepository.findById(command.taxProfileId.trim())
            ?: throw DomainRuleViolation("Tax profile does not exist.")

        return TaxProfileResult(profile)
    }
}

class TaxCreateProfileUseCase(
    private val profileRepository: TaxProfileRepository,
    private val rateRepository: TaxRateRepository,
    private val idGenerator: TaxIdGenerator,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxCreateProfileCommand): TaxProfileResult {
        assertCanManageTax(command.actorEffectivePermissions)

        val now = Instant.now(clock)
        val code = command.code.normalizedTaxCode("Tax profile code")

        if (profileRepository.findByCode(code) != null) {
            throw DomainRuleViolation("Tax profile code already exists: $code.")
        }

        val taxRate = command.taxRateCode
            ?.normalizedTaxCode("Tax rate code")
            ?.let { rateCode ->
                rateRepository.findByCode(rateCode)
                    ?: throw DomainRuleViolation("Tax rate does not exist: $rateCode.")
            }

        assertTreatmentMatchesRate(command.treatment, taxRate)

        val profile = TaxProfile(
            id = idGenerator.newId("taxp"),
            code = code,
            name = command.name.requiredTrimmed("Tax profile name"),
            treatment = command.treatment,
            status = command.status,
            taxRate = taxRate,
            sriTaxCode = command.sriTaxCode.normalizedNullable(),
            sriRateCode = command.sriRateCode.normalizedNullable(),
            legalBasis = command.legalBasis.requiredTrimmed("Tax profile legal basis"),
            effectiveFrom = command.effectiveFrom,
            effectiveTo = command.effectiveTo,
            source = TaxSource.PLATFORM_ADMIN,
            createdAt = now,
            updatedAt = now,
        )

        profileRepository.create(profile)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_PROFILE_CREATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = profile.id,
                before = emptyMap(),
                after = profile.toAuditMap(),
                reason = command.reason.normalizedNullable(),
                createdAt = now,
            )
        )

        return TaxProfileResult(profile)
    }
}

class TaxUpdateProfileUseCase(
    private val profileRepository: TaxProfileRepository,
    private val rateRepository: TaxRateRepository,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxUpdateProfileCommand): TaxProfileResult {
        assertCanManageTax(command.actorEffectivePermissions)

        val now = Instant.now(clock)
        val reason = command.reason.requiredTrimmed("Tax profile update reason")

        val current = profileRepository.findById(command.taxProfileId.trim())
            ?: throw DomainRuleViolation("Tax profile does not exist.")

        val resolvedTreatment = command.treatment ?: current.treatment

        val resolvedTaxRate = when {
            command.clearTaxRate -> null
            command.taxRateCode != null -> {
                val rateCode = command.taxRateCode.normalizedTaxCode("Tax rate code")
                rateRepository.findByCode(rateCode)
                    ?: throw DomainRuleViolation("Tax rate does not exist: $rateCode.")
            }

            else -> current.taxRate
        }

        assertTreatmentMatchesRate(resolvedTreatment, resolvedTaxRate)

        val updated = current.copy(
            name = command.name?.requiredTrimmed("Tax profile name") ?: current.name,
            treatment = resolvedTreatment,
            status = command.status ?: current.status,
            taxRate = resolvedTaxRate,
            sriTaxCode = when {
                command.clearSriTaxCode -> null
                command.sriTaxCode != null -> command.sriTaxCode.normalizedNullable()
                else -> current.sriTaxCode
            },
            sriRateCode = when {
                command.clearSriRateCode -> null
                command.sriRateCode != null -> command.sriRateCode.normalizedNullable()
                else -> current.sriRateCode
            },
            legalBasis = command.legalBasis?.requiredTrimmed("Tax profile legal basis") ?: current.legalBasis,
            effectiveFrom = command.effectiveFrom ?: current.effectiveFrom,
            effectiveTo = when {
                command.clearEffectiveTo -> null
                command.effectiveTo != null -> command.effectiveTo
                else -> current.effectiveTo
            },
            updatedAt = now,
            version = current.version + 1,
        )

        profileRepository.update(updated)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_PROFILE_UPDATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = updated.id,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return TaxProfileResult(updated)
    }
}

class TaxUpdateOrganizationSettingsUseCase(
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val profileRepository: TaxProfileRepository,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxUpdateOrganizationSettingsCommand): OrganizationTaxSettingsMutationResult {
        assertCanUpdateOrganizationTaxSettings(command.actorEffectivePermissions)

        val now = Instant.now(clock)
        val reason = command.reason.requiredTrimmed("Organization tax settings update reason")

        val current = settingsRepository.findByOrganizationId(command.organizationId.trim())
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")

        val enabledCodes = command.enabledTaxProfileCodes
            ?.map { it.normalizedTaxCode("Enabled tax profile code") }
            ?.toSet()
            ?: current.enabledTaxProfileCodes

        val defaultCode = command.defaultTaxProfileCode
            ?.normalizedTaxCode("Default tax profile code")
            ?: current.defaultTaxProfileCode

        if (defaultCode !in enabledCodes) {
            throw DomainRuleViolation("Default tax profile must be included in enabled tax profile codes.")
        }

        assertEnabledProfilesExistAndAreActive(enabledCodes, profileRepository)

        val updated = current.copy(
            regime = command.regime ?: current.regime,
            defaultTaxProfileCode = defaultCode,
            enabledTaxProfileCodes = enabledCodes,
            allowTaxInclusivePrices = command.allowTaxInclusivePrices ?: current.allowTaxInclusivePrices,
            allowManualLineDiscounts = command.allowManualLineDiscounts ?: current.allowManualLineDiscounts,
            requireTaxProfileForCatalogItems = command.requireTaxProfileForCatalogItems
                ?: current.requireTaxProfileForCatalogItems,
            status = command.status ?: current.status,
            updatedAt = now,
            updatedBy = command.actorUserId,
            version = current.version + 1,
        )

        settingsRepository.update(updated)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.ORGANIZATION_TAX_SETTINGS_UPDATED,
                actorUserId = command.actorUserId,
                organizationId = updated.organizationId,
                targetId = updated.id,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return OrganizationTaxSettingsMutationResult(updated)
    }
}

private fun assertCanManageTax(effectivePermissions: Set<String>) {
    PermissionRules.assertCanPerform(effectivePermissions, PermissionCatalog.TAX_MANAGE)
}

private fun assertCanUpdateOrganizationTaxSettings(effectivePermissions: Set<String>) {
    val allowed = PermissionCatalog.ALL in effectivePermissions ||
            PermissionCatalog.TAX_MANAGE in effectivePermissions ||
            PermissionCatalog.TAX_SETTINGS_UPDATE_ORGANIZATION_REGIME in effectivePermissions

    if (!allowed) {
        throw DomainRuleViolation(
            "Missing any required permission: ${PermissionCatalog.TAX_MANAGE}, " +
                    "${PermissionCatalog.TAX_SETTINGS_UPDATE_ORGANIZATION_REGIME}."
        )
    }
}

private fun assertEnabledProfilesExistAndAreActive(
    codes: Set<String>,
    profileRepository: TaxProfileRepository,
) {
    if (codes.isEmpty()) {
        throw DomainRuleViolation("At least one tax profile must be enabled.")
    }

    codes.forEach { code ->
        val profile = profileRepository.findByCode(code)
            ?: throw DomainRuleViolation("Enabled tax profile does not exist: $code.")

        if (profile.status.name != "ACTIVE") {
            throw DomainRuleViolation("Enabled tax profile must be active: $code.")
        }
    }
}

private fun assertTreatmentMatchesRate(treatment: TaxTreatment, taxRate: TaxRate?) {
    val zero = BigDecimal.ZERO.setScale(TaxRate.RATE_SCALE, RoundingMode.HALF_UP)

    when (treatment) {
        TaxTreatment.IVA_FULL,
        TaxTreatment.IVA_REDUCED_OR_SPECIAL -> {
            if (taxRate == null) {
                throw DomainRuleViolation("Taxed profiles require a tax rate.")
            }
            if (taxRate.rate.compareTo(zero) <= 0) {
                throw DomainRuleViolation("Taxed profiles require a positive tax rate.")
            }
        }

        TaxTreatment.IVA_ZERO -> {
            if (taxRate == null) {
                throw DomainRuleViolation("IVA zero profile requires a zero tax rate.")
            }
            if (taxRate.rate.compareTo(zero) != 0) {
                throw DomainRuleViolation("IVA zero profile requires a zero tax rate.")
            }
        }

        TaxTreatment.EXEMPT_IVA,
        TaxTreatment.NOT_SUBJECT_TO_IVA,
        TaxTreatment.NO_TAX_INTERNAL -> {
            if (taxRate != null) {
                throw DomainRuleViolation("Non-taxed profiles must not reference a tax rate.")
            }
        }
    }
}

private fun String.requiredTrimmed(label: String): String =
    trim().takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$label cannot be blank.")

private fun String.normalizedTaxCode(label: String): String =
    requiredTrimmed(label).lowercase()

private fun String?.normalizedNullable(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private fun String.normalizedTaxRateString(): String =
    toTaxRateDecimal().toPlainString()

private fun String.toTaxRateDecimal(): BigDecimal =
    try {
        BigDecimal(trim()).setScale(TaxRate.RATE_SCALE, RoundingMode.HALF_UP)
    } catch (_: NumberFormatException) {
        throw DomainRuleViolation("Tax rate value is invalid.")
    }

private fun TaxRate.toAuditMap(): Map<String, String?> =
    mapOf(
        "id" to id,
        "code" to code,
        "name" to name,
        "kind" to kind.name,
        "rate" to rate.toPlainString(),
        "status" to status.name,
        "sriTaxCode" to sriTaxCode,
        "sriRateCode" to sriRateCode,
        "legalBasis" to legalBasis,
        "effectiveFrom" to effectiveFrom.toString(),
        "effectiveTo" to effectiveTo?.toString(),
        "source" to source.name,
        "version" to version.toString(),
    )

private fun TaxProfile.toAuditMap(): Map<String, String?> =
    mapOf(
        "id" to id,
        "code" to code,
        "name" to name,
        "treatment" to treatment.name,
        "status" to status.name,
        "taxRateCode" to taxRate?.code,
        "sriTaxCode" to sriTaxCode,
        "sriRateCode" to sriRateCode,
        "legalBasis" to legalBasis,
        "effectiveFrom" to effectiveFrom.toString(),
        "effectiveTo" to effectiveTo?.toString(),
        "source" to source.name,
        "version" to version.toString(),
    )

private fun OrganizationTaxSettings.toAuditMap(): Map<String, String?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "regime" to regime.name,
        "defaultTaxProfileCode" to defaultTaxProfileCode,
        "enabledTaxProfileCodes" to enabledTaxProfileCodes.sorted().joinToString(","),
        "allowTaxInclusivePrices" to allowTaxInclusivePrices.toString(),
        "allowManualLineDiscounts" to allowManualLineDiscounts.toString(),
        "requireTaxProfileForCatalogItems" to requireTaxProfileForCatalogItems.toString(),
        "status" to status.name,
        "updatedBy" to updatedBy,
        "version" to version.toString(),
    )