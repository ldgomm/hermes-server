package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class GetAdminActivityUseCase(
    private val repository: AdminActivityMutationRepository,
) {
    fun execute(command: GetAdminActivityCommand): AdminBusinessActivityResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.ACTIVITIES_VIEW)
        val organizationId = command.organizationId.required("Organization id")
        val activityId = command.activityId.required("Activity id")
        val activity = repository.findActivity(organizationId, activityId)
            ?: throw DomainRuleViolation("Business activity does not exist.")
        return AdminBusinessActivityResult(activity)
    }
}

class CreateAdminActivityUseCase(
    private val repository: AdminActivityMutationRepository,
    private val idGenerator: AdminBusinessIdGenerator = UuidAdminBusinessIdGenerator(),
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateAdminActivityCommand): AdminBusinessActivityResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.ACTIVITIES_CREATE)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Activity creation reason")

        val code = command.code.normalizedActivityCode()
        if (repository.existsActivityCode(organizationId, code)) {
            throw DomainRuleViolation("Business activity code already exists: $code.")
        }

        val name = command.name.required("Activity name")
        val description = command.description.normalizedNullable()
        val activityType = command.activityType.normalizedActivityType()
        val workflowMode = command.workflowMode.normalizedWorkflowMode()
        val status = command.status.normalizedActivityStatus()

        validateWorkflowConfiguration(
            workflowMode = workflowMode,
            status = status,
            requiresScheduling = command.requiresScheduling,
        )

        val activity = repository.createActivity(
            AdminActivityCreateDraft(
                id = idGenerator.newId("act"),
                organizationId = organizationId,
                code = code,
                name = name,
                description = description,
                activityType = activityType,
                workflowMode = workflowMode,
                status = status,
                requiresScheduling = command.requiresScheduling,
                tracksInventory = command.tracksInventory,
                allowsReceivables = command.allowsReceivables,
                sortOrder = command.sortOrder,
                createdBy = actorUserId,
                createdAt = now,
            )
        )

        auditLogger.log(
            AdminBusinessAuditEvent(
                action = AdminBusinessAuditAction.ACTIVITY_CREATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = activity.id,
                after = activity.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessActivityResult(activity)
    }
}

class UpdateAdminActivityUseCase(
    private val repository: AdminActivityMutationRepository,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UpdateAdminActivityCommand): AdminBusinessActivityResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.ACTIVITIES_UPDATE)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val activityId = command.activityId.required("Activity id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Activity update reason")

        val current = repository.findActivity(organizationId, activityId)
            ?: throw DomainRuleViolation("Business activity does not exist.")
        if (current.archived) {
            throw DomainRuleViolation("Archived business activity cannot be updated.")
        }

        if (command.description != null && command.clearDescription) {
            throw DomainRuleViolation("Activity description cannot be set and cleared at the same time.")
        }

        val normalizedCode = command.code?.normalizedActivityCode()
        if (normalizedCode != null && normalizedCode != current.code &&
            repository.existsActivityCode(organizationId, normalizedCode, excludeActivityId = activityId)
        ) {
            throw DomainRuleViolation("Business activity code already exists: $normalizedCode.")
        }

        val targetWorkflowMode = command.workflowMode?.normalizedWorkflowMode() ?: current.workflowMode
        val targetStatus = current.status.normalizedActivityStatus()
        val targetRequiresScheduling = command.requiresScheduling ?: current.requiresScheduling
        validateWorkflowConfiguration(
            workflowMode = targetWorkflowMode,
            status = targetStatus,
            requiresScheduling = targetRequiresScheduling,
        )

        val patch = AdminActivityUpdatePatch(
            organizationId = organizationId,
            activityId = activityId,
            code = normalizedCode.takeIfChanged(current.code),
            name = command.name?.required("Activity name").takeIfChanged(current.name),
            description = when {
                command.clearDescription -> null
                command.description != null -> command.description.normalizedNullable()
                else -> null
            },
            changeDescription = command.clearDescription || command.description.normalizedNullable() != null && command.description.normalizedNullable() != current.description,
            activityType = command.activityType?.normalizedActivityType().takeIfChanged(current.activityType),
            workflowMode = command.workflowMode?.normalizedWorkflowMode().takeIfChanged(current.workflowMode),
            requiresScheduling = command.requiresScheduling.takeIfChanged(current.requiresScheduling),
            tracksInventory = command.tracksInventory.takeIfChanged(current.tracksInventory),
            allowsReceivables = command.allowsReceivables.takeIfChanged(current.allowsReceivables),
            sortOrder = command.sortOrder.takeIfChanged(current.sortOrder),
            updatedBy = actorUserId,
            updatedAt = now,
        )

        if (!patch.hasChanges()) {
            throw DomainRuleViolation("Activity update does not contain changes.")
        }

        val updated = repository.updateActivity(patch)
        auditLogger.log(
            AdminBusinessAuditEvent(
                action = AdminBusinessAuditAction.ACTIVITY_UPDATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = activityId,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessActivityResult(updated)
    }
}

class ChangeAdminActivityStatusUseCase(
    private val repository: AdminActivityMutationRepository,
    private val auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun activate(command: ChangeAdminActivityStatusCommand): AdminBusinessActivityResult = changeStatus(
        command = command.copy(targetStatus = ACTIVE_STATUS),
        action = AdminBusinessAuditAction.ACTIVITY_ACTIVATED,
    )

    fun deactivate(command: ChangeAdminActivityStatusCommand): AdminBusinessActivityResult = changeStatus(
        command = command.copy(targetStatus = PAUSED_STATUS),
        action = AdminBusinessAuditAction.ACTIVITY_DEACTIVATED,
    )

    private fun changeStatus(
        command: ChangeAdminActivityStatusCommand,
        action: AdminBusinessAuditAction,
    ): AdminBusinessActivityResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.ACTIVITIES_UPDATE)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val activityId = command.activityId.required("Activity id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Activity status change reason")
        val targetStatus = command.targetStatus.normalizedActivityStatus()

        val current = repository.findActivity(organizationId, activityId)
            ?: throw DomainRuleViolation("Business activity does not exist.")
        if (current.archived) {
            throw DomainRuleViolation("Archived business activity cannot change status.")
        }
        if (current.status == targetStatus) {
            throw DomainRuleViolation("Business activity is already $targetStatus.")
        }

        validateWorkflowConfiguration(
            workflowMode = current.workflowMode,
            status = targetStatus,
            requiresScheduling = current.requiresScheduling,
        )

        val updated = repository.updateActivityStatus(
            AdminActivityStatusPatch(
                organizationId = organizationId,
                activityId = activityId,
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
                targetId = activityId,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminBusinessActivityResult(updated)
    }
}

private const val ACTIVE_STATUS = "active"
private const val PAUSED_STATUS = "paused"

private val allowedActivityTypes = setOf("restaurant", "retail", "services", "tourism", "rental", "mixed", "custom")
private val allowedWorkflowModes = setOf("quick_sale", "order", "reservation", "service_order", "rental")
private val allowedActivityStatuses = setOf("draft", "active", "paused", "archived")

private fun String.normalizedActivityCode(): String = required("Activity code")
    .lowercase()
    .replace(Regex("[^a-z0-9_\\-]+"), "_")
    .replace(Regex("_+"), "_")
    .trim('_', '-')
    .takeIf { it.isNotBlank() }
    ?: throw DomainRuleViolation("Activity code is invalid.")

private fun String.normalizedActivityType(): String {
    val value = required("Activity type").lowercase().replace('-', '_')
    if (value !in allowedActivityTypes) {
        throw DomainRuleViolation("Unsupported activity type: $value.")
    }
    return value
}

private fun String.normalizedWorkflowMode(): String {
    val value = required("Activity workflow mode").lowercase().replace('-', '_')
    if (value !in allowedWorkflowModes) {
        throw DomainRuleViolation("Unsupported activity workflow mode: $value.")
    }
    return value
}

private fun String.normalizedActivityStatus(): String {
    val value = required("Activity status").lowercase().replace('-', '_')
    if (value !in allowedActivityStatuses) {
        throw DomainRuleViolation("Unsupported activity status: $value.")
    }
    return value
}

private fun validateWorkflowConfiguration(
    workflowMode: String,
    status: String,
    requiresScheduling: Boolean,
) {
    if (status == ACTIVE_STATUS && workflowMode == "reservation" && !requiresScheduling) {
        throw DomainRuleViolation("Reservation activity requires scheduling enabled before activation.")
    }
}

private fun String?.normalizedNullable(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun String?.takeIfChanged(current: String?): String? = when {
    this == null -> null
    this == current -> null
    else -> this
}

private fun Boolean?.takeIfChanged(current: Boolean): Boolean? = when {
    this == null -> null
    this == current -> null
    else -> this
}

private fun Int?.takeIfChanged(current: Int): Int? = when {
    this == null -> null
    this == current -> null
    else -> this
}

private fun AdminBusinessActivitySummary.toAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "organizationId" to organizationId,
    "code" to code,
    "name" to name,
    "description" to description,
    "activityType" to activityType,
    "workflowMode" to workflowMode,
    "status" to status,
    "requiresScheduling" to requiresScheduling.toString(),
    "tracksInventory" to tracksInventory.toString(),
    "allowsReceivables" to allowsReceivables.toString(),
    "sortOrder" to sortOrder.toString(),
)
