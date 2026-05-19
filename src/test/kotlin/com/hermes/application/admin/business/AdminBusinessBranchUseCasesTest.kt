package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminBusinessBranchUseCasesTest {
    private val now: Instant = Instant.parse("2026-05-19T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `creates branch with normalized code and geo location`() {
        val repository = InMemoryAdminBranchMutationRepository13A4()
        val auditLogger = RecordingAdminBusinessAuditLogger13A4()
        val useCase = CreateAdminBranchUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "br_created" },
            auditLogger = auditLogger,
            clock = clock,
        )

        val result = useCase.execute(
            CreateAdminBranchCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.BRANCHES_CREATE),
                code = " Principal 01 ",
                name = "Sucursal principal",
                type = "main",
                status = "active",
                location = AdminBranchLocationCommand(
                    countryCode = "ec",
                    province = "Pichincha",
                    city = "Mejía",
                    sector = "Tambillo",
                    addressLine = "El Murco",
                    latitude = -0.40,
                    longitude = -78.55,
                    privacyMode = "approximate_public",
                ),
                businessHoursId = "hours_main",
                reason = "Alta de sucursal piloto",
            )
        )

        assertEquals("br_created", result.branch.id)
        assertEquals("principal_01", result.branch.code)
        assertEquals("main", result.branch.type)
        assertEquals("EC", result.branch.location?.countryCode)
        assertEquals(-0.40, result.branch.location?.latitude)
        assertEquals(-78.55, result.branch.location?.longitude)
        assertEquals(AdminBusinessAuditAction.BRANCH_CREATED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects second active main branch`() {
        val repository = InMemoryAdminBranchMutationRepository13A4().apply {
            branches["br_main"] = branch13A4(id = "br_main", code = "main", type = "main", status = "active")
        }
        val useCase = CreateAdminBranchUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "br_other" },
            clock = clock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminBranchCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.BRANCHES_CREATE),
                    code = "main2",
                    name = "Otra matriz",
                    type = "main",
                    status = "active",
                    reason = "Duplicar matriz",
                )
            )
        }
    }

    @Test
    fun `updates branch and clears business hours`() {
        val repository = InMemoryAdminBranchMutationRepository13A4().apply {
            branches["br_1"] = branch13A4(
                id = "br_1",
                code = "principal",
                name = "Principal",
                businessHoursId = "hours_old",
            )
        }
        val auditLogger = RecordingAdminBusinessAuditLogger13A4()
        val useCase = UpdateAdminBranchUseCase(repository, auditLogger, clock)

        val result = useCase.execute(
            UpdateAdminBranchCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.BRANCHES_UPDATE),
                branchId = "br_1",
                name = "Sucursal principal actualizada",
                clearBusinessHoursId = true,
                reason = "Actualizar sucursal",
            )
        )

        assertEquals("Sucursal principal actualizada", result.branch.name)
        assertEquals(null, result.branch.businessHoursId)
        assertEquals(AdminBusinessAuditAction.BRANCH_UPDATED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects deactivating branch with active emission points`() {
        val repository = InMemoryAdminBranchMutationRepository13A4().apply {
            branches["br_1"] = branch13A4(id = "br_1", code = "principal", status = "active")
            branches["br_2"] = branch13A4(id = "br_2", code = "norte", status = "active")
            activeEmissionPointBranchIds += "br_1"
        }
        val useCase = ChangeAdminBranchStatusUseCase(repository, clock = clock)

        assertFailsWith<DomainRuleViolation> {
            useCase.deactivate(
                ChangeAdminBranchStatusCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.BRANCHES_UPDATE),
                    branchId = "br_1",
                    targetStatus = "inactive",
                    reason = "Cerrar temporalmente",
                )
            )
        }
    }

    @Test
    fun `deactivates branch when another active branch exists`() {
        val repository = InMemoryAdminBranchMutationRepository13A4().apply {
            branches["br_1"] = branch13A4(id = "br_1", code = "principal", status = "active")
            branches["br_2"] = branch13A4(id = "br_2", code = "norte", status = "active")
        }
        val auditLogger = RecordingAdminBusinessAuditLogger13A4()
        val useCase = ChangeAdminBranchStatusUseCase(repository, auditLogger, clock)

        val result = useCase.deactivate(
            ChangeAdminBranchStatusCommand(
                organizationId = "org_1",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.BRANCHES_UPDATE),
                branchId = "br_1",
                targetStatus = "inactive",
                reason = "Cierre temporal",
            )
        )

        assertEquals("inactive", result.branch.status)
        assertEquals(AdminBusinessAuditAction.BRANCH_DEACTIVATED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects location with latitude without longitude`() {
        val repository = InMemoryAdminBranchMutationRepository13A4()
        val useCase = CreateAdminBranchUseCase(
            repository = repository,
            idGenerator = AdminBusinessIdGenerator { "br_bad" },
            clock = clock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminBranchCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = setOf(PermissionCatalog.BRANCHES_CREATE),
                    code = "bad",
                    name = "Bad",
                    location = AdminBranchLocationCommand(latitude = -0.40),
                    reason = "Datos inválidos",
                )
            )
        }
    }
}

private class InMemoryAdminBranchMutationRepository13A4 : AdminBranchMutationRepository {
    val branches: MutableMap<String, AdminBusinessBranchSummary> = linkedMapOf()
    val activeEmissionPointBranchIds: MutableSet<String> = mutableSetOf()

    override fun findBranch(organizationId: String, branchId: String): AdminBusinessBranchSummary? =
        branches[branchId]?.takeIf { it.organizationId == organizationId }

    override fun existsBranchCode(organizationId: String, code: String, excludeBranchId: String?): Boolean =
        branches.values.any { it.organizationId == organizationId && it.code == code && it.id != excludeBranchId }

    override fun hasActiveMainBranch(organizationId: String, excludeBranchId: String?): Boolean =
        branches.values.any {
            it.organizationId == organizationId && it.type == "main" && it.status == "active" && it.id != excludeBranchId
        }

    override fun countActiveBranches(organizationId: String, excludeBranchId: String?): Int =
        branches.values.count { it.organizationId == organizationId && it.status == "active" && it.id != excludeBranchId }

    override fun hasActiveEmissionPoints(organizationId: String, branchId: String): Boolean =
        branchId in activeEmissionPointBranchIds

    override fun createBranch(draft: AdminBranchCreateDraft): AdminBusinessBranchSummary {
        val branch = AdminBusinessBranchSummary(
            id = draft.id,
            organizationId = draft.organizationId,
            code = draft.code,
            name = draft.name,
            type = draft.type,
            status = draft.status,
            location = draft.location,
            businessHoursId = draft.businessHoursId,
            createdAt = draft.createdAt,
            updatedAt = draft.createdAt,
        )
        branches[branch.id] = branch
        return branch
    }

    override fun updateBranch(patch: AdminBranchUpdatePatch): AdminBusinessBranchSummary {
        val current = findBranch(patch.organizationId, patch.branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        val updated = current.copy(
            code = patch.code ?: current.code,
            name = patch.name ?: current.name,
            type = patch.type ?: current.type,
            location = if (patch.changeLocation) patch.location else current.location,
            businessHoursId = if (patch.changeBusinessHoursId) patch.businessHoursId else current.businessHoursId,
            updatedAt = patch.updatedAt,
        )
        branches[updated.id] = updated
        return updated
    }

    override fun updateBranchStatus(patch: AdminBranchStatusPatch): AdminBusinessBranchSummary {
        val current = findBranch(patch.organizationId, patch.branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        val updated = current.copy(status = patch.status, updatedAt = patch.updatedAt)
        branches[updated.id] = updated
        return updated
    }
}

private class RecordingAdminBusinessAuditLogger13A4 : AdminBusinessAuditLogger {
    val events: MutableList<AdminBusinessAuditEvent> = mutableListOf()
    override fun log(event: AdminBusinessAuditEvent) {
        events += event
    }
}

private fun branch13A4(
    id: String,
    organizationId: String = "org_1",
    code: String = "principal",
    name: String = "Sucursal principal",
    type: String = "branch",
    status: String = "active",
    location: AdminBranchLocation? = null,
    businessHoursId: String? = null,
): AdminBusinessBranchSummary = AdminBusinessBranchSummary(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    type = type,
    status = status,
    location = location,
    businessHoursId = businessHoursId,
)
