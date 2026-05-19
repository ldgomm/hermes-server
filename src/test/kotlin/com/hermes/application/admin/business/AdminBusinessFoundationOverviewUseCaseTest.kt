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

class AdminBusinessFoundationOverviewUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-19T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `returns ready overview when business foundation is complete`() {
        val repository = InMemoryAdminBusinessFoundationRepository13A6().apply {
            taxSettings = true
            sriSettings = true
            ownerOrAdmin = true
            activities += activity13A6(status = "active")
            branches += branch13A6(status = "active")
            emissionPoints += emissionPoint13A6(status = "active")
        }
        val useCase = GetAdminBusinessFoundationOverviewUseCase(repository, clock)

        val result = useCase.execute(command13A6())

        assertTrue(result.ready)
        assertEquals(AdminBusinessReadinessStatus.READY, result.overallStatus)
        assertEquals(1, result.counts.activeActivities)
        assertEquals(1, result.counts.activeBranches)
        assertEquals(1, result.counts.activeEmissionPoints)
        assertEquals(0, result.counts.warningChecks)
        assertEquals(0, result.counts.blockedChecks)
        assertEquals(emptyList(), result.nextActions)
    }

    @Test
    fun `returns blocked overview when required foundation is missing`() {
        val repository = InMemoryAdminBusinessFoundationRepository13A6().apply {
            taxSettings = true
            sriSettings = true
            ownerOrAdmin = true
            activities += activity13A6(status = "active")
            // no active branch: required blocker
            emissionPoints += emissionPoint13A6(status = "active")
        }
        val useCase = GetAdminBusinessFoundationOverviewUseCase(repository, clock)

        val result = useCase.execute(command13A6())

        assertEquals(AdminBusinessReadinessStatus.BLOCKED, result.overallStatus)
        assertEquals(0, result.counts.activeBranches)
        assertEquals(1, result.counts.blockedChecks)
        assertTrue(result.nextActions.any { it.code == AdminBusinessReadinessCheckCode.ACTIVE_BRANCH_EXISTS.name })
    }

    @Test
    fun `returns warning overview when optional setup is missing`() {
        val repository = InMemoryAdminBusinessFoundationRepository13A6().apply {
            activities += activity13A6(status = "active")
            branches += branch13A6(status = "active")
            emissionPoints += emissionPoint13A6(status = "active")
            // tax/SRI/owner-admin are optional warnings in 13A readiness
        }
        val useCase = GetAdminBusinessFoundationOverviewUseCase(repository, clock)

        val result = useCase.execute(command13A6())

        assertEquals(AdminBusinessReadinessStatus.WARNING, result.overallStatus)
        assertEquals(3, result.counts.warningChecks)
        assertEquals(3, result.nextActions.size)
    }

    @Test
    fun `rejects actor without business view permission`() {
        val repository = InMemoryAdminBusinessFoundationRepository13A6()
        val useCase = GetAdminBusinessFoundationOverviewUseCase(repository, clock)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(command13A6(permissions = emptySet()))
        }
    }

    private fun command13A6(
        permissions: Set<String> = setOf(PermissionCatalog.ORGANIZATION_VIEW),
    ): GetAdminBusinessFoundationOverviewCommand = GetAdminBusinessFoundationOverviewCommand(
        organizationId = "org_1",
        actorUserId = "usr_1",
        actorEffectivePermissions = permissions,
    )
}

private class InMemoryAdminBusinessFoundationRepository13A6 : AdminBusinessRepository {
    var business: AdminBusinessProfile? = AdminBusinessProfile(
        id = "org_1",
        countryCode = "EC",
        taxId = "1790000000001",
        legalName = "Hermes Demo S.A.",
        commercialName = "Hermes Demo",
        status = "active",
        ownerUserId = "usr_1",
        defaultCurrency = "USD",
        timezone = "America/Guayaquil",
    )
    val activities: MutableList<AdminBusinessActivitySummary> = mutableListOf()
    val branches: MutableList<AdminBusinessBranchSummary> = mutableListOf()
    val emissionPoints: MutableList<AdminBusinessEmissionPointSummary> = mutableListOf()
    var taxSettings: Boolean = false
    var sriSettings: Boolean = false
    var ownerOrAdmin: Boolean = false

    override fun findBusiness(organizationId: String): AdminBusinessProfile? =
        business?.takeIf { it.id == organizationId }

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> =
        activities.filter { it.organizationId == organizationId }

    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> =
        branches.filter { it.organizationId == organizationId }

    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> =
        emissionPoints.filter { it.organizationId == organizationId }

    override fun hasTaxSettings(organizationId: String): Boolean = taxSettings
    override fun hasSriSettings(organizationId: String): Boolean = sriSettings
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = ownerOrAdmin
}

private fun activity13A6(
    id: String = "act_1",
    status: String = "active",
): AdminBusinessActivitySummary = AdminBusinessActivitySummary(
    id = id,
    organizationId = "org_1",
    code = "restaurant",
    name = "Restaurante",
    activityType = "restaurant",
    workflowMode = "order",
    status = status,
    requiresScheduling = false,
    tracksInventory = true,
    allowsReceivables = true,
)

private fun branch13A6(
    id: String = "br_1",
    status: String = "active",
): AdminBusinessBranchSummary = AdminBusinessBranchSummary(
    id = id,
    organizationId = "org_1",
    code = "main",
    name = "Matriz",
    type = "main",
    status = status,
)

private fun emissionPoint13A6(
    id: String = "ep_1",
    status: String = "active",
): AdminBusinessEmissionPointSummary = AdminBusinessEmissionPointSummary(
    id = id,
    organizationId = "org_1",
    branchId = "br_1",
    establishmentCode = "001",
    emissionPointCode = "001",
    displayName = "Caja principal",
    status = status,
)
