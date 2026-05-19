package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class GetAdminEmissionPointUseCase(
    private val repository: AdminEmissionPointMutationRepository,
) {
    fun execute(command: GetAdminEmissionPointCommand): AdminBusinessEmissionPointResult {
        assertCanViewEmissionPoints(command.actorEffectivePermissions)
        val organizationId = command.organizationId.required("Organization id")
        val emissionPointId = command.emissionPointId.required("Emission point id")
        val emissionPoint = repository.findEmissionPoint(organizationId, emissionPointId)
            ?: throw DomainRuleViolation("Emission point does not exist.")
        return AdminBusinessEmissionPointResult(emissionPoint)
    }
}

class CreateAdminEmissionPointUseCase(
    private val repository: AdminEmissionPointMutationRepository,
    private val idGenerator: AdminBusinessIdGenerator,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateAdminEmissionPointCommand): AdminBusinessEmissionPointResult {
        assertCanManageEmissionPoints(command.actorEffectivePermissions)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Emission point creation reason")
        val branchId = command.branchId.required("Branch id")
        val branch = repository.findBranch(organizationId, branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        if (!branch.active) {
            throw DomainRuleViolation("Emission point can only be created for an active branch.")
        }

        val establishmentCode = command.establishmentCode.normalizedSriThreeDigitCode("Establishment code")
        val emissionPointCode = command.emissionPointCode.normalizedSriThreeDigitCode("Emission point code")
        if (repository.existsEmissionPointCodes(organizationId, establishmentCode, emissionPointCode)) {
            throw DomainRuleViolation("Emission point code already exists for this organization.")
        }

        val status = command.status.normalizedEmissionPointStatus()
        if (status == ARCHIVED_EMISSION_POINT_STATUS) {
            throw DomainRuleViolation("Emission point cannot be created as archived.")
        }

        val displayName = command.displayName.required("Emission point display name")
            .assertMaxLength("Emission point display name", 128)

        val created = repository.createEmissionPoint(
            AdminEmissionPointCreateDraft(
                id = idGenerator.newId("ep"),
                organizationId = organizationId,
                branchId = branchId,
                establishmentCode = establishmentCode,
                emissionPointCode = emissionPointCode,
                displayName = displayName,
                status = status,
                createdBy = actorUserId,
                createdAt = now,
            )
        )

        auditLogger.log(
            AdminBusinessAuditEvent(
                action = AdminBusinessAuditAction.EMISSION_POINT_CREATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = created.id,
                after = created.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessEmissionPointResult(created)
    }
}

class UpdateAdminEmissionPointUseCase(
    private val repository: AdminEmissionPointMutationRepository,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UpdateAdminEmissionPointCommand): AdminBusinessEmissionPointResult {
        assertCanManageEmissionPoints(command.actorEffectivePermissions)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val emissionPointId = command.emissionPointId.required("Emission point id")
        val reason = command.reason.required("Emission point update reason")

        val current = repository.findEmissionPoint(organizationId, emissionPointId)
            ?: throw DomainRuleViolation("Emission point does not exist.")
        if (current.archived) {
            throw DomainRuleViolation("Archived emission point cannot be updated.")
        }

        val targetBranchId = command.branchId?.required("Branch id")
        if (targetBranchId != null && targetBranchId != current.branchId) {
            val branch = repository.findBranch(organizationId, targetBranchId)
                ?: throw DomainRuleViolation("Branch does not exist.")
            if (!branch.active) {
                throw DomainRuleViolation("Emission point can only be moved to an active branch.")
            }
        }

        val targetEstablishmentCode = command.establishmentCode?.normalizedSriThreeDigitCode("Establishment code")
        val targetEmissionPointCode = command.emissionPointCode?.normalizedSriThreeDigitCode("Emission point code")
        val resolvedEstablishmentCode = targetEstablishmentCode ?: current.establishmentCode
        val resolvedEmissionPointCode = targetEmissionPointCode ?: current.emissionPointCode
        val codesChanged = resolvedEstablishmentCode != current.establishmentCode ||
                resolvedEmissionPointCode != current.emissionPointCode

        if (codesChanged && repository.existsEmissionPointCodes(
                organizationId = organizationId,
                establishmentCode = resolvedEstablishmentCode,
                emissionPointCode = resolvedEmissionPointCode,
                excludeEmissionPointId = emissionPointId,
            )
        ) {
            throw DomainRuleViolation("Emission point code already exists for this organization.")
        }

        val patch = AdminEmissionPointUpdatePatch(
            organizationId = organizationId,
            emissionPointId = emissionPointId,
            branchId = targetBranchId.takeIfChanged(current.branchId),
            establishmentCode = targetEstablishmentCode.takeIfChanged(current.establishmentCode),
            emissionPointCode = targetEmissionPointCode.takeIfChanged(current.emissionPointCode),
            displayName = command.displayName?.required("Emission point display name")
                ?.assertMaxLength("Emission point display name", 128)
                .takeIfChanged(current.displayName),
            updatedBy = actorUserId,
            updatedAt = now,
        )

        if (!patch.hasChanges()) {
            throw DomainRuleViolation("Emission point update does not contain changes.")
        }

        val updated = repository.updateEmissionPoint(patch)
        auditLogger.log(
            AdminBusinessAuditEvent(
                action = AdminBusinessAuditAction.EMISSION_POINT_UPDATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = emissionPointId,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessEmissionPointResult(updated)
    }
}

class ChangeAdminEmissionPointStatusUseCase(
    private val repository: AdminEmissionPointMutationRepository,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun activate(command: ChangeAdminEmissionPointStatusCommand): AdminBusinessEmissionPointResult = changeStatus(
        command = command.copy(targetStatus = ACTIVE_EMISSION_POINT_STATUS),
        action = AdminBusinessAuditAction.EMISSION_POINT_ACTIVATED,
    )

    fun deactivate(command: ChangeAdminEmissionPointStatusCommand): AdminBusinessEmissionPointResult = changeStatus(
        command = command.copy(targetStatus = INACTIVE_EMISSION_POINT_STATUS),
        action = AdminBusinessAuditAction.EMISSION_POINT_DEACTIVATED,
    )

    private fun changeStatus(
        command: ChangeAdminEmissionPointStatusCommand,
        action: AdminBusinessAuditAction,
    ): AdminBusinessEmissionPointResult {
        assertCanManageEmissionPoints(command.actorEffectivePermissions)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val emissionPointId = command.emissionPointId.required("Emission point id")
        val reason = command.reason.required("Emission point status change reason")
        val targetStatus = command.targetStatus.normalizedEmissionPointStatus()

        val current = repository.findEmissionPoint(organizationId, emissionPointId)
            ?: throw DomainRuleViolation("Emission point does not exist.")
        if (current.archived) {
            throw DomainRuleViolation("Archived emission point cannot change status.")
        }
        if (current.status == targetStatus) {
            throw DomainRuleViolation("Emission point is already $targetStatus.")
        }

        if (targetStatus == ACTIVE_EMISSION_POINT_STATUS) {
            val branch = repository.findBranch(organizationId, current.branchId)
                ?: throw DomainRuleViolation("Emission point branch does not exist.")
            if (!branch.active) {
                throw DomainRuleViolation("Emission point cannot be activated while its branch is inactive.")
            }
        }

        val updated = repository.updateEmissionPointStatus(
            AdminEmissionPointStatusPatch(
                organizationId = organizationId,
                emissionPointId = emissionPointId,
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
                targetId = emissionPointId,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessEmissionPointResult(updated)
    }
}

private const val ACTIVE_EMISSION_POINT_STATUS = "active"
private const val INACTIVE_EMISSION_POINT_STATUS = "inactive"
private const val ARCHIVED_EMISSION_POINT_STATUS = "archived"

private val allowedEmissionPointStatuses = setOf(
    ACTIVE_EMISSION_POINT_STATUS,
    INACTIVE_EMISSION_POINT_STATUS,
    ARCHIVED_EMISSION_POINT_STATUS,
)

private fun assertCanManageEmissionPoints(effectivePermissions: Set<String>) {
    if (!canPerformAny(effectivePermissions, setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE))) {
        throw DomainRuleViolation("Missing required permission: ${PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE}.")
    }
}

private fun String.normalizedSriThreeDigitCode(label: String): String {
    val value = required(label)
    if (!value.all(Char::isDigit)) {
        throw DomainRuleViolation("$label must contain only digits.")
    }
    if (value.length > 3) {
        throw DomainRuleViolation("$label must contain at most 3 digits.")
    }
    return value.padStart(3, '0')
}

private fun String.normalizedEmissionPointStatus(): String {
    val value = required("Emission point status").lowercase().replace('-', '_')
    if (value !in allowedEmissionPointStatuses) {
        throw DomainRuleViolation("Unsupported emission point status: $value.")
    }
    return value
}

private fun String.assertMaxLength(label: String, maxLength: Int): String {
    if (length > maxLength) throw DomainRuleViolation("$label must contain at most $maxLength characters.")
    return this
}

private fun String?.takeIfChanged(current: String?): String? = when {
    this == null -> null
    this == current -> null
    else -> this
}

private fun AdminBusinessEmissionPointSummary.toAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "branchId" to branchId,
    "establishmentCode" to establishmentCode,
    "emissionPointCode" to emissionPointCode,
    "displayName" to displayName,
    "status" to status,
)
