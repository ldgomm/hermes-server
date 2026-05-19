package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AdminBusinessReadinessUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-19T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `returns READY when all required and recommended checks pass`() {
        val repository = FakeAdminBusinessRepository().apply {
            business = business()
            activities += activity(status = "active")
            branches += branch(status = "active")
            emissionPoints += emissionPoint(status = "active")
            taxSettings = true
            sriSettings = true
            activeOwnerOrAdmin = true
        }

        val result = GetAdminBusinessReadinessUseCase(repository, clock).execute(command())

        assertEquals(AdminBusinessReadinessStatus.READY, result.overallStatus)
        assertEquals(true, result.ready)
        assertEquals(now, result.generatedAt)
    }

    @Test
    fun `returns BLOCKED when core operational setup is missing`() {
        val repository = FakeAdminBusinessRepository().apply {
            business = business(status = "active")
            taxSettings = true
            sriSettings = true
            activeOwnerOrAdmin = true
        }

        val result = GetAdminBusinessReadinessUseCase(repository, clock).execute(command())

        assertEquals(AdminBusinessReadinessStatus.BLOCKED, result.overallStatus)
        assertEquals(
            AdminBusinessReadinessStatus.BLOCKED,
            result.checks.first { it.code == AdminBusinessReadinessCheckCode.ACTIVE_ACTIVITY_EXISTS }.status,
        )
        assertEquals(
            AdminBusinessReadinessStatus.BLOCKED,
            result.checks.first { it.code == AdminBusinessReadinessCheckCode.ACTIVE_BRANCH_EXISTS }.status,
        )
    }

    @Test
    fun `returns WARNING when electronic invoicing recommendations are missing`() {
        val repository = FakeAdminBusinessRepository().apply {
            business = business(status = "active")
            activities += activity(status = "active")
            branches += branch(status = "active")
            taxSettings = false
            sriSettings = false
            activeOwnerOrAdmin = false
        }

        val result = GetAdminBusinessReadinessUseCase(repository, clock).execute(command())

        assertEquals(AdminBusinessReadinessStatus.WARNING, result.overallStatus)
        assertEquals(
            AdminBusinessReadinessStatus.WARNING,
            result.checks.first { it.code == AdminBusinessReadinessCheckCode.TAX_SETTINGS_INITIALIZED }.status,
        )
        assertEquals(
            AdminBusinessReadinessStatus.WARNING,
            result.checks.first { it.code == AdminBusinessReadinessCheckCode.SRI_SETTINGS_CONFIGURED }.status,
        )
    }

    @Test
    fun `rejects actor without readiness permission`() {
        val repository = FakeAdminBusinessRepository().apply { business = business(status = "active") }

        assertFailsWith<DomainRuleViolation> {
            GetAdminBusinessReadinessUseCase(repository, clock).execute(
                command(permissions = setOf(PermissionCatalog.SALES_VIEW)),
            )
        }
    }

    private fun command(
        permissions: Set<String> = setOf(PermissionCatalog.ORGANIZATION_VIEW),
    ): GetAdminBusinessReadinessCommand = GetAdminBusinessReadinessCommand(
        organizationId = "org_1",
        actorUserId = "usr_1",
        actorEffectivePermissions = permissions,
    )

    private fun business(status: String = "active"): AdminBusinessProfile = AdminBusinessProfile(
        id = "org_1",
        countryCode = "EC",
        taxId = "1790000000001",
        legalName = "Hermes Demo S.A.",
        commercialName = "Hermes Demo",
        status = status,
        ownerUserId = "usr_1",
        createdAt = now,
        updatedAt = now,
    )

    private fun activity(status: String): AdminBusinessActivitySummary = AdminBusinessActivitySummary(
        id = "act_1",
        organizationId = "org_1",
        code = "restaurant",
        name = "Restaurante",
        activityType = "restaurant",
        workflowMode = "order",
        status = status,
        requiresScheduling = false,
        tracksInventory = true,
        allowsReceivables = true,
        sortOrder = 1,
    )

    private fun branch(status: String): AdminBusinessBranchSummary = AdminBusinessBranchSummary(
        id = "br_1",
        organizationId = "org_1",
        code = "001",
        name = "Sucursal principal",
        type = "main",
        status = status,
    )

    private fun emissionPoint(status: String): AdminBusinessEmissionPointSummary = AdminBusinessEmissionPointSummary(
        id = "ep_1",
        organizationId = "org_1",
        branchId = "br_1",
        establishmentCode = "001",
        emissionPointCode = "001",
        displayName = "Caja principal",
        status = status,
    )
}

private class FakeAdminBusinessRepository : AdminBusinessRepository {
    var business: AdminBusinessProfile? = null
    val activities: MutableList<AdminBusinessActivitySummary> = mutableListOf()
    val branches: MutableList<AdminBusinessBranchSummary> = mutableListOf()
    val emissionPoints: MutableList<AdminBusinessEmissionPointSummary> = mutableListOf()
    var taxSettings: Boolean = false
    var sriSettings: Boolean = false
    var activeOwnerOrAdmin: Boolean = false

    override fun findBusiness(organizationId: String): AdminBusinessProfile? = business?.takeIf { it.id == organizationId }
    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> = activities.filter { it.organizationId == organizationId }
    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> = branches.filter { it.organizationId == organizationId }
    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> = emissionPoints.filter { it.organizationId == organizationId }
    override fun hasTaxSettings(organizationId: String): Boolean = taxSettings
    override fun hasSriSettings(organizationId: String): Boolean = sriSettings
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = activeOwnerOrAdmin
}
