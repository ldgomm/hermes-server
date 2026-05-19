package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminBusinessEmissionPointUseCasesTest {
    private val now: Instant = Instant.parse("2026-05-19T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `creates emission point with normalized numeric codes`() {
        val repository = InMemoryAdminEmissionPointRepository13A5().apply {
            branches["br_1"] = branch13A5(id = "br_1", status = "active")
        }
        val auditLogger = RecordingAdminBusinessAuditLogger13A5()
        val useCase = CreateAdminEmissionPointUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "ep_created" },
            auditLogger = auditLogger,
            clock = clock,
        )

        val result = useCase.execute(
            CreateAdminEmissionPointCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE),
                branchId = "br_1",
                establishmentCode = "1",
                emissionPointCode = "2",
                displayName = "Caja principal",
                status = "active",
                reason = "Configurar punto de emisión principal",
            )
        )

        assertEquals("ep_created", result.emissionPoint.id)
        assertEquals("001", result.emissionPoint.establishmentCode)
        assertEquals("002", result.emissionPoint.emissionPointCode)
        assertEquals("001-002", result.emissionPoint.fullCode)
        assertEquals(AdminBusinessAuditAction.EMISSION_POINT_CREATED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects duplicate establishment and emission point code`() {
        val repository = InMemoryAdminEmissionPointRepository13A5().apply {
            branches["br_1"] = branch13A5(id = "br_1", status = "active")
            emissionPoints["ep_1"] =
                emissionPoint13A5(id = "ep_1", branchId = "br_1", establishmentCode = "001", emissionPointCode = "001")
        }
        val useCase = CreateAdminEmissionPointUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "ep_duplicate" },
            clock = clock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminEmissionPointCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE),
                    branchId = "br_1",
                    establishmentCode = "001",
                    emissionPointCode = "001",
                    displayName = "Caja duplicada",
                    reason = "Duplicar código",
                )
            )
        }
    }

    @Test
    fun `rejects create active emission point for inactive branch`() {
        val repository = InMemoryAdminEmissionPointRepository13A5().apply {
            branches["br_1"] = branch13A5(id = "br_1", status = "inactive")
        }
        val useCase = CreateAdminEmissionPointUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "ep_bad" },
            clock = clock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminEmissionPointCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE),
                    branchId = "br_1",
                    establishmentCode = "001",
                    emissionPointCode = "002",
                    displayName = "Caja",
                    reason = "Sucursal inactiva",
                )
            )
        }
    }

    @Test
    fun `updates emission point branch and display name`() {
        val repository = InMemoryAdminEmissionPointRepository13A5().apply {
            branches["br_1"] = branch13A5(id = "br_1", status = "active")
            branches["br_2"] = branch13A5(id = "br_2", status = "active")
            emissionPoints["ep_1"] = emissionPoint13A5(id = "ep_1", branchId = "br_1", displayName = "Caja antigua")
        }
        val auditLogger = RecordingAdminBusinessAuditLogger13A5()
        val useCase = UpdateAdminEmissionPointUseCase(repository, auditLogger, clock)

        val result = useCase.execute(
            UpdateAdminEmissionPointCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE),
                emissionPointId = "ep_1",
                branchId = "br_2",
                displayName = "Caja nueva",
                reason = "Mover caja",
            )
        )

        assertEquals("br_2", result.emissionPoint.branchId)
        assertEquals("Caja nueva", result.emissionPoint.displayName)
        assertEquals(AdminBusinessAuditAction.EMISSION_POINT_UPDATED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects activating emission point when branch is inactive`() {
        val repository = InMemoryAdminEmissionPointRepository13A5().apply {
            branches["br_1"] = branch13A5(id = "br_1", status = "inactive")
            emissionPoints["ep_1"] = emissionPoint13A5(id = "ep_1", branchId = "br_1", status = "inactive")
        }
        val useCase = ChangeAdminEmissionPointStatusUseCase(repository, clock = clock)

        assertFailsWith<DomainRuleViolation> {
            useCase.activate(
                ChangeAdminEmissionPointStatusCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE),
                    emissionPointId = "ep_1",
                    targetStatus = "active",
                    reason = "Activar sin sucursal activa",
                )
            )
        }
    }

    @Test
    fun `deactivates emission point`() {
        val repository = InMemoryAdminEmissionPointRepository13A5().apply {
            branches["br_1"] = branch13A5(id = "br_1", status = "active")
            emissionPoints["ep_1"] = emissionPoint13A5(id = "ep_1", branchId = "br_1", status = "active")
        }
        val auditLogger = RecordingAdminBusinessAuditLogger13A5()
        val useCase = ChangeAdminEmissionPointStatusUseCase(repository, auditLogger, clock)

        val result = useCase.deactivate(
            ChangeAdminEmissionPointStatusCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE),
                emissionPointId = "ep_1",
                targetStatus = "inactive",
                reason = "Cierre temporal",
            )
        )

        assertEquals("inactive", result.emissionPoint.status)
        assertEquals(AdminBusinessAuditAction.EMISSION_POINT_DEACTIVATED, auditLogger.events.single().action)
    }
}

private class InMemoryAdminEmissionPointRepository13A5 : AdminEmissionPointMutationRepository {
    val branches: MutableMap<String, AdminBusinessBranchSummary> = linkedMapOf()
    val emissionPoints: MutableMap<String, AdminBusinessEmissionPointSummary> = linkedMapOf()

    override fun findEmissionPoint(
        organizationId: String,
        emissionPointId: String
    ): AdminBusinessEmissionPointSummary? =
        emissionPoints[emissionPointId]?.takeIf { it.organizationId == organizationId }

    override fun findBranch(organizationId: String, branchId: String): AdminBusinessBranchSummary? =
        branches[branchId]?.takeIf { it.organizationId == organizationId }

    override fun existsEmissionPointCodes(
        organizationId: String,
        establishmentCode: String,
        emissionPointCode: String,
        excludeEmissionPointId: String?,
    ): Boolean = emissionPoints.values.any {
        it.organizationId == organizationId &&
                it.establishmentCode == establishmentCode &&
                it.emissionPointCode == emissionPointCode &&
                it.id != excludeEmissionPointId
    }

    override fun createEmissionPoint(draft: AdminEmissionPointCreateDraft): AdminBusinessEmissionPointSummary {
        val emissionPoint = AdminBusinessEmissionPointSummary(
            id = draft.id,
            organizationId = draft.organizationId,
            branchId = draft.branchId,
            establishmentCode = draft.establishmentCode,
            emissionPointCode = draft.emissionPointCode,
            displayName = draft.displayName,
            status = draft.status,
            createdAt = draft.createdAt,
            updatedAt = draft.createdAt,
        )
        emissionPoints[emissionPoint.id] = emissionPoint
        return emissionPoint
    }

    override fun updateEmissionPoint(patch: AdminEmissionPointUpdatePatch): AdminBusinessEmissionPointSummary {
        val current = findEmissionPoint(patch.organizationId, patch.emissionPointId)
            ?: throw DomainRuleViolation("Emission point does not exist.")
        val updated = current.copy(
            branchId = patch.branchId ?: current.branchId,
            establishmentCode = patch.establishmentCode ?: current.establishmentCode,
            emissionPointCode = patch.emissionPointCode ?: current.emissionPointCode,
            displayName = patch.displayName ?: current.displayName,
            updatedAt = patch.updatedAt,
        )
        emissionPoints[updated.id] = updated
        return updated
    }

    override fun updateEmissionPointStatus(patch: AdminEmissionPointStatusPatch): AdminBusinessEmissionPointSummary {
        val current = findEmissionPoint(patch.organizationId, patch.emissionPointId)
            ?: throw DomainRuleViolation("Emission point does not exist.")
        val updated = current.copy(status = patch.status, updatedAt = patch.updatedAt)
        emissionPoints[updated.id] = updated
        return updated
    }
}

private class RecordingAdminBusinessAuditLogger13A5 : AdminBusinessAuditLogger {
    val events: MutableList<AdminBusinessAuditEvent> = mutableListOf()
    override fun log(event: AdminBusinessAuditEvent) {
        events += event
    }
}

private fun branch13A5(
    id: String,
    organizationId: String = "org_1",
    code: String = id,
    name: String = "Branch $id",
    status: String = "active",
): AdminBusinessBranchSummary = AdminBusinessBranchSummary(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    type = "branch",
    status = status,
)

private fun emissionPoint13A5(
    id: String,
    organizationId: String = "org_1",
    branchId: String = "br_1",
    establishmentCode: String = "001",
    emissionPointCode: String = "001",
    displayName: String = "Caja 1",
    status: String = "active",
): AdminBusinessEmissionPointSummary = AdminBusinessEmissionPointSummary(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    establishmentCode = establishmentCode,
    emissionPointCode = emissionPointCode,
    displayName = displayName,
    status = status,
)
