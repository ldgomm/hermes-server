package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdminBusinessActivityUseCasesTest {
    private val now: Instant = Instant.parse("2026-05-19T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `creates an active activity with normalized code`() {
        val repository = InMemoryAdminActivityMutationRepository()
        val auditLogger = RecordingAdminBusinessAuditLogger()
        val useCase = CreateAdminActivityUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "act_created" },
            auditLogger = auditLogger,
            clock = clock,
        )

        val result = useCase.execute(
            CreateAdminActivityCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.ACTIVITIES_CREATE),
                code = " Restaurante Principal ",
                name = "Restaurante",
                description = "Servicio de comida",
                activityType = "restaurant",
                workflowMode = "order",
                status = "active",
                requiresScheduling = false,
                tracksInventory = true,
                allowsReceivables = true,
                sortOrder = 10,
                reason = "Alta de actividad piloto",
            )
        )

        assertEquals("act_created", result.activity.id)
        assertEquals("restaurante_principal", result.activity.code)
        assertEquals("Restaurante", result.activity.name)
        assertEquals("active", result.activity.status)
        assertEquals(1, auditLogger.events.size)
        assertEquals(AdminBusinessAuditAction.ACTIVITY_CREATED, auditLogger.events.first().action)
    }

    @Test
    fun `rejects duplicated activity code in same organization`() {
        val repository = InMemoryAdminActivityMutationRepository().apply {
            activities["act_1"] = activity(id = "act_1", code = "retail")
        }
        val useCase = CreateAdminActivityUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "act_2" },
            clock = clock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminActivityCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.ACTIVITIES_CREATE),
                    code = "retail",
                    name = "Retail duplicado",
                    activityType = "retail",
                    workflowMode = "quick_sale",
                    reason = "Duplicado",
                )
            )
        }
    }

    @Test
    fun `updates activity fields and clears description`() {
        val repository = InMemoryAdminActivityMutationRepository().apply {
            activities["act_1"] = activity(id = "act_1", code = "restaurante", description = "Anterior")
        }
        val auditLogger = RecordingAdminBusinessAuditLogger()
        val useCase = UpdateAdminActivityUseCase(repository, auditLogger, clock)

        val result = useCase.execute(
            UpdateAdminActivityCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.ACTIVITIES_UPDATE),
                activityId = "act_1",
                name = "Restaurante actualizado",
                clearDescription = true,
                tracksInventory = true,
                sortOrder = 2,
                reason = "Ajuste administrativo",
            )
        )

        assertEquals("Restaurante actualizado", result.activity.name)
        assertEquals(null, result.activity.description)
        assertTrue(result.activity.tracksInventory)
        assertEquals(2, result.activity.sortOrder)
        assertEquals(AdminBusinessAuditAction.ACTIVITY_UPDATED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects activation of reservation activity without scheduling enabled`() {
        val repository = InMemoryAdminActivityMutationRepository().apply {
            activities["act_1"] = activity(
                id = "act_1",
                code = "turismo",
                activityType = "tourism",
                workflowMode = "reservation",
                status = "paused",
                requiresScheduling = false,
            )
        }
        val useCase = ChangeAdminActivityStatusUseCase(repository, clock = clock)

        assertFailsWith<DomainRuleViolation> {
            useCase.activate(
                ChangeAdminActivityStatusCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.ACTIVITIES_UPDATE),
                    activityId = "act_1",
                    targetStatus = "active",
                    reason = "Activar turismo",
                )
            )
        }
    }

    @Test
    fun `deactivates active activity to paused`() {
        val repository = InMemoryAdminActivityMutationRepository().apply {
            activities["act_1"] = activity(id = "act_1", status = "active")
        }
        val auditLogger = RecordingAdminBusinessAuditLogger()
        val useCase = ChangeAdminActivityStatusUseCase(repository, auditLogger, clock)

        val result = useCase.deactivate(
            ChangeAdminActivityStatusCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.ACTIVITIES_UPDATE),
                activityId = "act_1",
                targetStatus = "paused",
                reason = "Cierre temporal",
            )
        )

        assertEquals("paused", result.activity.status)
        assertEquals(AdminBusinessAuditAction.ACTIVITY_DEACTIVATED, auditLogger.events.single().action)
    }
}

private class InMemoryAdminActivityMutationRepository : AdminActivityMutationRepository {
    val activities: MutableMap<String, AdminBusinessActivitySummary> = linkedMapOf()

    override fun findActivity(organizationId: String, activityId: String): AdminBusinessActivitySummary? =
        activities[activityId]?.takeIf { it.organizationId == organizationId }

    override fun existsActivityCode(
        organizationId: String,
        code: String,
        excludeActivityId: String?,
    ): Boolean = activities.values.any {
        it.organizationId == organizationId && it.code == code && it.id != excludeActivityId
    }

    override fun createActivity(draft: AdminActivityCreateDraft): AdminBusinessActivitySummary {
        val activity = AdminBusinessActivitySummary(
            id = draft.id,
            organizationId = draft.organizationId,
            code = draft.code,
            name = draft.name,
            description = draft.description,
            activityType = draft.activityType,
            workflowMode = draft.workflowMode,
            status = draft.status,
            requiresScheduling = draft.requiresScheduling,
            tracksInventory = draft.tracksInventory,
            allowsReceivables = draft.allowsReceivables,
            sortOrder = draft.sortOrder,
            createdAt = draft.createdAt,
            updatedAt = draft.createdAt,
        )
        activities[activity.id] = activity
        return activity
    }

    override fun updateActivity(patch: AdminActivityUpdatePatch): AdminBusinessActivitySummary {
        val current = findActivity(patch.organizationId, patch.activityId)
            ?: throw DomainRuleViolation("Business activity does not exist.")
        val updated = current.copy(
            code = patch.code ?: current.code,
            name = patch.name ?: current.name,
            description = if (patch.changeDescription) patch.description else current.description,
            activityType = patch.activityType ?: current.activityType,
            workflowMode = patch.workflowMode ?: current.workflowMode,
            requiresScheduling = patch.requiresScheduling ?: current.requiresScheduling,
            tracksInventory = patch.tracksInventory ?: current.tracksInventory,
            allowsReceivables = patch.allowsReceivables ?: current.allowsReceivables,
            sortOrder = patch.sortOrder ?: current.sortOrder,
            updatedAt = patch.updatedAt,
        )
        activities[updated.id] = updated
        return updated
    }

    override fun updateActivityStatus(patch: AdminActivityStatusPatch): AdminBusinessActivitySummary {
        val current = findActivity(patch.organizationId, patch.activityId)
            ?: throw DomainRuleViolation("Business activity does not exist.")
        val updated = current.copy(status = patch.status, updatedAt = patch.updatedAt)
        activities[updated.id] = updated
        return updated
    }
}

private class RecordingAdminBusinessAuditLogger : AdminBusinessAuditLogger {
    val events: MutableList<AdminBusinessAuditEvent> = mutableListOf()
    override fun log(event: AdminBusinessAuditEvent) {
        events += event
    }
}

private fun activity(
    id: String,
    organizationId: String = "org_1",
    code: String = "restaurante",
    name: String = "Restaurante",
    description: String? = "Comida",
    activityType: String = "restaurant",
    workflowMode: String = "order",
    status: String = "active",
    requiresScheduling: Boolean = false,
    tracksInventory: Boolean = false,
    allowsReceivables: Boolean = true,
    sortOrder: Int = 1,
): AdminBusinessActivitySummary = AdminBusinessActivitySummary(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    description = description,
    activityType = activityType,
    workflowMode = workflowMode,
    status = status,
    requiresScheduling = requiresScheduling,
    tracksInventory = tracksInventory,
    allowsReceivables = allowsReceivables,
    sortOrder = sortOrder,
)
