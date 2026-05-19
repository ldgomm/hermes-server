package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class GetAdminBranchUseCase(
    private val repository: AdminBranchMutationRepository,
) {
    fun execute(command: GetAdminBranchCommand): AdminBusinessBranchResult {
        assertCanViewBranches(command.actorEffectivePermissions)
        val organizationId = command.organizationId.required("Organization id")
        val branchId = command.branchId.required("Branch id")
        val branch = repository.findBranch(organizationId, branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        return AdminBusinessBranchResult(branch)
    }
}

class CreateAdminBranchUseCase(
    private val repository: AdminBranchMutationRepository,
    private val idGenerator: AdminBusinessIdGenerator = UuidAdminBusinessIdGenerator(),
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateAdminBranchCommand): AdminBusinessBranchResult {
        assertCanCreateBranch(command.actorEffectivePermissions)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Branch creation reason")

        val code = command.code.normalizedBranchCode()
        if (repository.existsBranchCode(organizationId, code)) {
            throw DomainRuleViolation("Branch code already exists: $code.")
        }

        val name = command.name.required("Branch name").also { it.assertMaxLength("Branch name", 128) }
        val type = command.type.normalizedBranchType()
        val status = command.status.normalizedBranchStatus()
        val location = command.location?.toBranchLocation(requireContent = true)
        val businessHoursId = command.businessHoursId.normalizedNullable()

        if (status == ACTIVE_BRANCH_STATUS && type == MAIN_BRANCH_TYPE && repository.hasActiveMainBranch(organizationId)) {
            throw DomainRuleViolation("Only one active main branch is allowed per organization.")
        }

        val branch = repository.createBranch(
            AdminBranchCreateDraft(
                id = idGenerator.newId("br"),
                organizationId = organizationId,
                code = code,
                name = name,
                type = type,
                status = status,
                location = location,
                businessHoursId = businessHoursId,
                createdBy = actorUserId,
                createdAt = now,
            )
        )

        auditLogger.log(
            AdminBusinessAuditEvent(
                action = AdminBusinessAuditAction.BRANCH_CREATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = branch.id,
                after = branch.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessBranchResult(branch)
    }
}

class UpdateAdminBranchUseCase(
    private val repository: AdminBranchMutationRepository,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UpdateAdminBranchCommand): AdminBusinessBranchResult {
        assertCanUpdateBranch(command.actorEffectivePermissions)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val branchId = command.branchId.required("Branch id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Branch update reason")

        val current = repository.findBranch(organizationId, branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        if (current.archived) {
            throw DomainRuleViolation("Archived branch cannot be updated.")
        }

        if (command.location != null && command.clearLocation) {
            throw DomainRuleViolation("Branch location cannot be set and cleared at the same time.")
        }
        if (command.businessHoursId != null && command.clearBusinessHoursId) {
            throw DomainRuleViolation("Branch business hours cannot be set and cleared at the same time.")
        }

        val normalizedCode = command.code?.normalizedBranchCode()
        if (normalizedCode != null && normalizedCode != current.code &&
            repository.existsBranchCode(organizationId, normalizedCode, excludeBranchId = branchId)
        ) {
            throw DomainRuleViolation("Branch code already exists: $normalizedCode.")
        }

        val targetType = command.type?.normalizedBranchType() ?: current.type
        val targetStatus = current.status.normalizedBranchStatus()
        if (targetStatus == ACTIVE_BRANCH_STATUS && targetType == MAIN_BRANCH_TYPE && !current.main &&
            repository.hasActiveMainBranch(organizationId, excludeBranchId = branchId)
        ) {
            throw DomainRuleViolation("Only one active main branch is allowed per organization.")
        }

        val normalizedLocation = when {
            command.clearLocation -> null
            command.location != null -> command.location.toBranchLocation(requireContent = true)
            else -> null
        }
        val changeLocation =
            command.clearLocation || normalizedLocation != null && normalizedLocation != current.location

        val normalizedBusinessHoursId = when {
            command.clearBusinessHoursId -> null
            command.businessHoursId != null -> command.businessHoursId.normalizedNullable()
            else -> null
        }
        val changeBusinessHoursId = command.clearBusinessHoursId ||
                (command.businessHoursId != null && normalizedBusinessHoursId != current.businessHoursId)

        val patch = AdminBranchUpdatePatch(
            organizationId = organizationId,
            branchId = branchId,
            code = normalizedCode.takeIfChanged(current.code),
            name = command.name?.required("Branch name")?.also { it.assertMaxLength("Branch name", 128) }
                .takeIfChanged(current.name),
            type = command.type?.normalizedBranchType().takeIfChanged(current.type),
            location = normalizedLocation,
            changeLocation = changeLocation,
            businessHoursId = normalizedBusinessHoursId,
            changeBusinessHoursId = changeBusinessHoursId,
            updatedBy = actorUserId,
            updatedAt = now,
        )

        if (!patch.hasChanges()) {
            throw DomainRuleViolation("Branch update does not contain changes.")
        }

        val updated = repository.updateBranch(patch)
        auditLogger.log(
            AdminBusinessAuditEvent(
                action = AdminBusinessAuditAction.BRANCH_UPDATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = branchId,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessBranchResult(updated)
    }
}

class ChangeAdminBranchStatusUseCase(
    private val repository: AdminBranchMutationRepository,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun activate(command: ChangeAdminBranchStatusCommand): AdminBusinessBranchResult = changeStatus(
        command = command.copy(targetStatus = ACTIVE_BRANCH_STATUS),
        action = AdminBusinessAuditAction.BRANCH_ACTIVATED,
    )

    fun deactivate(command: ChangeAdminBranchStatusCommand): AdminBusinessBranchResult = changeStatus(
        command = command.copy(targetStatus = INACTIVE_BRANCH_STATUS),
        action = AdminBusinessAuditAction.BRANCH_DEACTIVATED,
    )

    private fun changeStatus(
        command: ChangeAdminBranchStatusCommand,
        action: AdminBusinessAuditAction,
    ): AdminBusinessBranchResult {
        assertCanUpdateBranch(command.actorEffectivePermissions)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val branchId = command.branchId.required("Branch id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Branch status change reason")
        val targetStatus = command.targetStatus.normalizedBranchStatus()

        val current = repository.findBranch(organizationId, branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        if (current.archived) {
            throw DomainRuleViolation("Archived branch cannot change status.")
        }
        if (current.status == targetStatus) {
            throw DomainRuleViolation("Branch is already $targetStatus.")
        }

        if (targetStatus == ACTIVE_BRANCH_STATUS && current.main &&
            repository.hasActiveMainBranch(organizationId, excludeBranchId = branchId)
        ) {
            throw DomainRuleViolation("Only one active main branch is allowed per organization.")
        }

        if (targetStatus == INACTIVE_BRANCH_STATUS) {
            if (repository.countActiveBranches(organizationId, excludeBranchId = branchId) == 0) {
                throw DomainRuleViolation("At least one active branch is required before deactivating this branch.")
            }
            if (repository.hasActiveEmissionPoints(organizationId, branchId)) {
                throw DomainRuleViolation("Branch cannot be deactivated while it has active emission points.")
            }
        }

        val updated = repository.updateBranchStatus(
            AdminBranchStatusPatch(
                organizationId = organizationId,
                branchId = branchId,
                status = targetStatus,
                updatedBy = actorUserId,
                updatedAt = now,
            )
        )

        auditLogger.log(
            AdminBusinessAuditEvent(
                action = action,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = branchId,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessBranchResult(updated)
    }
}

private const val ACTIVE_BRANCH_STATUS = "active"
private const val INACTIVE_BRANCH_STATUS = "inactive"
private const val MAIN_BRANCH_TYPE = "main"

private val allowedBranchTypes = setOf("main", "branch", "warehouse", "mobile", "virtual")
private val allowedBranchStatuses = setOf("active", "inactive", "archived")
private val allowedLocationPrivacyModes = setOf("private", "approximate_public", "exact_public", "hidden")

private fun assertCanCreateBranch(effectivePermissions: Set<String>) {
    if (!canPerformAny(
            effectivePermissions,
            setOf(PermissionCatalog.BRANCHES_CREATE, PermissionCatalog.SETTINGS_BRANCHES_MANAGE)
        )
    ) {
        throw DomainRuleViolation(
            "Missing any required permission: ${PermissionCatalog.BRANCHES_CREATE}, ${PermissionCatalog.SETTINGS_BRANCHES_MANAGE}."
        )
    }
}

private fun assertCanUpdateBranch(effectivePermissions: Set<String>) {
    if (!canPerformAny(
            effectivePermissions,
            setOf(PermissionCatalog.BRANCHES_UPDATE, PermissionCatalog.SETTINGS_BRANCHES_MANAGE)
        )
    ) {
        throw DomainRuleViolation(
            "Missing any required permission: ${PermissionCatalog.BRANCHES_UPDATE}, ${PermissionCatalog.SETTINGS_BRANCHES_MANAGE}."
        )
    }
}

private fun String.normalizedBranchCode(): String = required("Branch code")
    .lowercase()
    .replace(Regex("[^a-z0-9_\\-]+"), "_")
    .replace(Regex("_+"), "_")
    .trim('_', '-')
    .takeIf { it.isNotBlank() }
    ?.also { it.assertMaxLength("Branch code", 16) }
    ?: throw DomainRuleViolation("Branch code is invalid.")

private fun String.normalizedBranchType(): String {
    val value = required("Branch type").lowercase().replace('-', '_')
    if (value !in allowedBranchTypes) {
        throw DomainRuleViolation("Unsupported branch type: $value.")
    }
    return value
}

private fun String.normalizedBranchStatus(): String {
    val value = required("Branch status").lowercase().replace('-', '_')
    if (value !in allowedBranchStatuses) {
        throw DomainRuleViolation("Unsupported branch status: $value.")
    }
    return value
}

private fun AdminBranchLocationCommand.toBranchLocation(requireContent: Boolean): AdminBranchLocation? {
    val countryCode = countryCode.normalizedNullable()?.uppercase()
    val province = province.normalizedNullable()
    val city = city.normalizedNullable()
    val sector = sector.normalizedNullable()
    val addressLine = addressLine.normalizedNullable()
    val privacyMode = privacyMode.normalizedNullable()?.lowercase()?.replace('-', '_')

    if (privacyMode != null && privacyMode !in allowedLocationPrivacyModes) {
        throw DomainRuleViolation("Unsupported branch location privacy mode: $privacyMode.")
    }
    if ((latitude == null) != (longitude == null)) {
        throw DomainRuleViolation("Branch latitude and longitude must be provided together.")
    }
    latitude?.let {
        if (it !in -90.0..90.0) throw DomainRuleViolation("Branch latitude is out of range.")
    }
    longitude?.let {
        if (it !in -180.0..180.0) throw DomainRuleViolation("Branch longitude is out of range.")
    }

    val location = AdminBranchLocation(
        countryCode = countryCode,
        province = province,
        city = city,
        sector = sector,
        addressLine = addressLine,
        latitude = latitude,
        longitude = longitude,
        privacyMode = privacyMode,
    )

    val empty = listOf(
        location.countryCode,
        location.province,
        location.city,
        location.sector,
        location.addressLine,
        location.privacyMode,
    ).all { it == null } && location.latitude == null && location.longitude == null

    if (empty && requireContent) {
        throw DomainRuleViolation("Branch location cannot be empty.")
    }
    return location.takeUnless { empty }
}

private fun String?.normalizedNullable(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun String.assertMaxLength(label: String, maxLength: Int): String {
    if (length > maxLength) throw DomainRuleViolation("$label must contain at most $maxLength characters.")
    return this
}

private fun String?.takeIfChanged(current: String?): String? = when {
    this == null -> null
    this == current -> null
    else -> this
}

private fun AdminBusinessBranchSummary.toAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "organizationId" to organizationId,
    "code" to code,
    "name" to name,
    "type" to type,
    "status" to status,
    "businessHoursId" to businessHoursId,
    "location" to location?.toAuditValue(),
)

private fun AdminBranchLocation.toAuditValue(): String = listOfNotNull(
    countryCode,
    province,
    city,
    sector,
    addressLine,
    latitude?.toString(),
    longitude?.toString(),
    privacyMode,
).joinToString(separator = "|")
