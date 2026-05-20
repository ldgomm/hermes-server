package com.hermes.application.admin.tax

import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.*
import java.time.Clock
import java.time.Instant

data class SearchAdminTaxRatesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val query: String? = null,
    val kind: TaxKind? = null,
    val statuses: Set<TaxRateStatus> = emptySet(),
    val effectiveAt: Instant? = null,
    val limit: Int = 100,
)

data class SearchAdminTaxProfilesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val query: String? = null,
    val treatment: TaxTreatment? = null,
    val statuses: Set<TaxProfileStatus> = emptySet(),
    val effectiveAt: Instant? = null,
    val limit: Int = 100,
)

data class GetAdminTaxReadinessCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class AdminTaxRatesResult(
    val rates: List<TaxRate>,
)

data class AdminTaxProfilesResult(
    val profiles: List<TaxProfile>,
)

data class AdminTaxReadinessResult(
    val organizationId: String,
    val ready: Boolean,
    val status: AdminTaxReadinessStatus,
    val checkedAt: Instant,
    val settings: OrganizationTaxSettings?,
    val checks: List<AdminTaxReadinessCheck>,
    val enabledProfileCount: Int,
    val activeEnabledProfileCount: Int,
    val missingProfileCodes: Set<String>,
    val nextActions: List<String>,
)

data class AdminTaxReadinessCheck(
    val code: String,
    val status: AdminTaxReadinessCheckStatus,
    val severity: AdminTaxReadinessSeverity,
    val message: String,
)

enum class AdminTaxReadinessStatus {
    READY,
    ACTION_REQUIRED,
}

enum class AdminTaxReadinessCheckStatus {
    PASSED,
    FAILED,
    WARNING,
}

enum class AdminTaxReadinessSeverity {
    INFO,
    WARNING,
    CRITICAL,
}

data class AdminTaxRateSearchQuery(
    val query: String? = null,
    val kind: TaxKind? = null,
    val statuses: Set<TaxRateStatus> = emptySet(),
    val effectiveAt: Instant? = null,
    val limit: Int = 100,
)

data class AdminTaxProfileSearchQuery(
    val query: String? = null,
    val treatment: TaxTreatment? = null,
    val statuses: Set<TaxProfileStatus> = emptySet(),
    val effectiveAt: Instant? = null,
    val limit: Int = 100,
)

interface AdminTaxRateQueryRepository {
    fun search(query: AdminTaxRateSearchQuery): List<TaxRate>
}

interface AdminTaxProfileQueryRepository {
    fun search(query: AdminTaxProfileSearchQuery): List<TaxProfile>
}

class SearchAdminTaxRatesUseCase(
    private val repository: AdminTaxRateQueryRepository,
) {
    fun execute(command: SearchAdminTaxRatesCommand): AdminTaxRatesResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)
        command.organizationId.requiredAdminTax("Organization id")

        return AdminTaxRatesResult(
            repository.search(
                AdminTaxRateSearchQuery(
                    query = command.query.normalizedNullable(),
                    kind = command.kind,
                    statuses = command.statuses,
                    effectiveAt = command.effectiveAt,
                    limit = command.limit.coerceIn(1, 250),
                ),
            ),
        )
    }
}

class SearchAdminTaxProfilesUseCase(
    private val repository: AdminTaxProfileQueryRepository,
) {
    fun execute(command: SearchAdminTaxProfilesCommand): AdminTaxProfilesResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)
        command.organizationId.requiredAdminTax("Organization id")

        return AdminTaxProfilesResult(
            repository.search(
                AdminTaxProfileSearchQuery(
                    query = command.query.normalizedNullable(),
                    treatment = command.treatment,
                    statuses = command.statuses,
                    effectiveAt = command.effectiveAt,
                    limit = command.limit.coerceIn(1, 250),
                ),
            ),
        )
    }
}

class GetAdminTaxReadinessUseCase(
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val profileRepository: TaxProfileRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GetAdminTaxReadinessCommand): AdminTaxReadinessResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)
        val organizationId = command.organizationId.requiredAdminTax("Organization id")
        val checkedAt = Instant.now(clock)
        val checks = mutableListOf<AdminTaxReadinessCheck>()
        val nextActions = linkedSetOf<String>()

        val settings = settingsRepository.findByOrganizationId(organizationId)
        if (settings == null) {
            checks += failed(
                code = "tax_settings_present",
                message = "Organization tax settings are missing.",
            )
            nextActions += "Initialize organization tax settings before assigning profiles or issuing documents."
            return AdminTaxReadinessResult(
                organizationId = organizationId,
                ready = false,
                status = AdminTaxReadinessStatus.ACTION_REQUIRED,
                checkedAt = checkedAt,
                settings = null,
                checks = checks,
                enabledProfileCount = 0,
                activeEnabledProfileCount = 0,
                missingProfileCodes = emptySet(),
                nextActions = nextActions.toList(),
            )
        }

        checks += passed("tax_settings_present", "Organization tax settings exist.")

        if (settings.status == OrganizationTaxSettingsStatus.ACTIVE) {
            checks += passed("tax_settings_active", "Organization tax settings are active.")
        } else {
            checks += failed("tax_settings_active", "Organization tax settings are ${settings.status}.")
            nextActions += "Activate tax settings before production use."
        }

        if (settings.enabledTaxProfileCodes.isNotEmpty()) {
            checks += passed("enabled_profiles_present", "At least one tax profile is enabled.")
        } else {
            checks += failed("enabled_profiles_present", "No tax profile is enabled for this organization.")
            nextActions += "Enable at least one tax profile."
        }

        if (settings.defaultTaxProfileCode in settings.enabledTaxProfileCodes) {
            checks += passed("default_profile_enabled", "Default tax profile is part of enabled profiles.")
        } else {
            checks += failed("default_profile_enabled", "Default tax profile is not enabled.")
            nextActions += "Add the default tax profile to enabled profiles."
        }

        val enabledProfiles = settings.enabledTaxProfileCodes.associateWith { profileRepository.findByCode(it) }
        val missingCodes = enabledProfiles.filterValues { it == null }.keys
        val presentProfiles = enabledProfiles.values.filterNotNull()
        val activeProfiles = presentProfiles.filter { it.status == TaxProfileStatus.ACTIVE }

        if (missingCodes.isEmpty()) {
            checks += passed("enabled_profiles_exist", "All enabled tax profile codes exist.")
        } else {
            checks += failed(
                code = "enabled_profiles_exist",
                message = "Some enabled tax profile codes do not exist: ${missingCodes.sorted().joinToString()}.",
            )
            nextActions += "Remove missing profile codes or create the missing profiles."
        }

        val inactiveCodes = presentProfiles.filterNot { it.status == TaxProfileStatus.ACTIVE }.map { it.code }.toSet()
        if (inactiveCodes.isEmpty()) {
            checks += passed("enabled_profiles_active", "All existing enabled tax profiles are active.")
        } else {
            checks += warning(
                code = "enabled_profiles_active",
                message = "Some enabled profiles are not active: ${inactiveCodes.sorted().joinToString()}.",
            )
            nextActions += "Review inactive, deprecated or archived enabled profiles."
        }

        val notEffectiveCodes = activeProfiles.filterNot { it.isEffectiveAt(checkedAt) }.map { it.code }.toSet()
        if (notEffectiveCodes.isEmpty()) {
            checks += passed(
                "enabled_profiles_effective",
                "All active enabled tax profiles are effective at the current instant."
            )
        } else {
            checks += failed(
                code = "enabled_profiles_effective",
                message = "Some active enabled profiles are outside their vigency window: ${
                    notEffectiveCodes.sorted().joinToString()
                }.",
            )
            nextActions += "Review effectiveFrom/effectiveTo for enabled profiles."
        }

        val rateIssues = activeProfiles
            .filter { profile -> profile.taxRate != null && runCatching { profile.taxRate.assertUsableAt(checkedAt) }.isFailure }
            .map { it.code }
            .toSet()
        if (rateIssues.isEmpty()) {
            checks += passed("profile_rates_usable", "Tax rates linked to active enabled profiles are usable now.")
        } else {
            checks += failed(
                code = "profile_rates_usable",
                message = "Some enabled profiles reference rates that are not currently usable: ${
                    rateIssues.sorted().joinToString()
                }.",
            )
            nextActions += "Review tax rate status and vigency for affected profiles."
        }

        val ready = checks.none { it.status == AdminTaxReadinessCheckStatus.FAILED }
        return AdminTaxReadinessResult(
            organizationId = organizationId,
            ready = ready,
            status = if (ready) AdminTaxReadinessStatus.READY else AdminTaxReadinessStatus.ACTION_REQUIRED,
            checkedAt = checkedAt,
            settings = settings,
            checks = checks,
            enabledProfileCount = settings.enabledTaxProfileCodes.size,
            activeEnabledProfileCount = activeProfiles.size,
            missingProfileCodes = missingCodes,
            nextActions = nextActions.toList(),
        )
    }
}

internal fun String.requiredAdminTax(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")

private fun String?.normalizedNullable(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun passed(code: String, message: String): AdminTaxReadinessCheck =
    AdminTaxReadinessCheck(
        code = code,
        status = AdminTaxReadinessCheckStatus.PASSED,
        severity = AdminTaxReadinessSeverity.INFO,
        message = message,
    )

private fun warning(code: String, message: String): AdminTaxReadinessCheck =
    AdminTaxReadinessCheck(
        code = code,
        status = AdminTaxReadinessCheckStatus.WARNING,
        severity = AdminTaxReadinessSeverity.WARNING,
        message = message,
    )

private fun failed(code: String, message: String): AdminTaxReadinessCheck =
    AdminTaxReadinessCheck(
        code = code,
        status = AdminTaxReadinessCheckStatus.FAILED,
        severity = AdminTaxReadinessSeverity.CRITICAL,
        message = message,
    )
