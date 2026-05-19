package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class GetAdminBusinessReadinessUseCase(
    private val repository: AdminBusinessRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GetAdminBusinessReadinessCommand): AdminBusinessReadinessResult {
        assertCanViewReadiness(command.actorEffectivePermissions)

        val organizationId = command.organizationId.required("Organization id")
        val business = repository.findBusiness(organizationId)
        val checks = buildList {
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.BUSINESS_EXISTS,
                    passed = business != null,
                    required = true,
                    blockedMessage = "Organization does not exist.",
                    okMessage = "Organization exists.",
                    action = "Create or select a valid organization.",
                )
            )

            if (business == null) {
                return@buildList
            }

            add(
                check(
                    code = AdminBusinessReadinessCheckCode.BUSINESS_ACTIVE,
                    passed = business.active,
                    required = true,
                    blockedMessage = "Organization is not active.",
                    okMessage = "Organization is active.",
                    action = "Activate the organization before operating.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.TAX_ID_PRESENT,
                    passed = business.taxId.isNotBlank(),
                    required = true,
                    blockedMessage = "Organization tax id is missing.",
                    okMessage = "Organization tax id is configured.",
                    action = "Configure the RUC/tax id.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.LEGAL_NAME_PRESENT,
                    passed = business.legalName.isNotBlank(),
                    required = true,
                    blockedMessage = "Organization legal name is missing.",
                    okMessage = "Organization legal name is configured.",
                    action = "Configure legal name.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.COMMERCIAL_NAME_PRESENT,
                    passed = business.commercialName.isNotBlank(),
                    required = true,
                    blockedMessage = "Organization commercial name is missing.",
                    okMessage = "Organization commercial name is configured.",
                    action = "Configure commercial name.",
                )
            )

            val activeActivities = repository.listActivities(organizationId).count { it.active }
            val activeBranches = repository.listBranches(organizationId).count { it.active }
            val activeEmissionPoints = repository.listEmissionPoints(organizationId).count { it.active }

            add(
                check(
                    code = AdminBusinessReadinessCheckCode.ACTIVE_ACTIVITY_EXISTS,
                    passed = activeActivities > 0,
                    required = true,
                    blockedMessage = "No active business activity exists.",
                    okMessage = "At least one active business activity exists.",
                    action = "Create or activate a business activity.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.ACTIVE_BRANCH_EXISTS,
                    passed = activeBranches > 0,
                    required = true,
                    blockedMessage = "No active branch exists.",
                    okMessage = "At least one active branch exists.",
                    action = "Create or activate a branch.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.ACTIVE_EMISSION_POINT_EXISTS,
                    passed = activeEmissionPoints > 0,
                    required = false,
                    blockedMessage = "No active emission point exists.",
                    okMessage = "At least one active emission point exists.",
                    action = "Configure an emission point before issuing electronic invoices.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.TAX_SETTINGS_INITIALIZED,
                    passed = repository.hasTaxSettings(organizationId),
                    required = false,
                    blockedMessage = "Organization tax settings are not initialized.",
                    okMessage = "Organization tax settings are initialized.",
                    action = "Initialize tax settings before selling with tax calculation.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.SRI_SETTINGS_CONFIGURED,
                    passed = repository.hasSriSettings(organizationId),
                    required = false,
                    blockedMessage = "SRI/electronic invoicing settings are not configured.",
                    okMessage = "SRI/electronic invoicing settings are configured.",
                    action = "Configure SRI settings in the electronic invoicing module when the business will issue electronic invoices.",
                )
            )
            add(
                check(
                    code = AdminBusinessReadinessCheckCode.OWNER_OR_ADMIN_CONFIGURED,
                    passed = repository.hasActiveOwnerOrAdminMembership(organizationId),
                    required = false,
                    blockedMessage = "No active owner/admin membership was found.",
                    okMessage = "Owner/admin access is configured.",
                    action = "Ensure the business has at least one active owner/admin user.",
                )
            )
        }

        return AdminBusinessReadinessResult(
            organizationId = organizationId,
            overallStatus = checks.overallStatus(),
            checks = checks,
            generatedAt = Instant.now(clock),
        )
    }

    private fun assertCanViewReadiness(effectivePermissions: Set<String>) {
        val allowed = canPerformAny(
            effectivePermissions,
            setOf(
                PermissionCatalog.ORGANIZATION_VIEW,
                PermissionCatalog.ORGANIZATION_UPDATE,
                PermissionCatalog.AUDIT_VIEW,
            ),
        )
        if (!allowed) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.ORGANIZATION_VIEW}, ${PermissionCatalog.AUDIT_VIEW}."
            )
        }
    }

    private fun check(
        code: AdminBusinessReadinessCheckCode,
        passed: Boolean,
        required: Boolean,
        blockedMessage: String,
        okMessage: String,
        action: String?,
    ): AdminBusinessReadinessCheck = AdminBusinessReadinessCheck(
        code = code,
        status = when {
            passed -> AdminBusinessReadinessStatus.READY
            required -> AdminBusinessReadinessStatus.BLOCKED
            else -> AdminBusinessReadinessStatus.WARNING
        },
        required = required,
        message = if (passed) okMessage else blockedMessage,
        action = action.takeUnless { passed },
    )

    private fun List<AdminBusinessReadinessCheck>.overallStatus(): AdminBusinessReadinessStatus = when {
        any { it.status == AdminBusinessReadinessStatus.BLOCKED } -> AdminBusinessReadinessStatus.BLOCKED
        any { it.status == AdminBusinessReadinessStatus.WARNING } -> AdminBusinessReadinessStatus.WARNING
        else -> AdminBusinessReadinessStatus.READY
    }
}
