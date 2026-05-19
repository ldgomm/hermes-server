package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminBusinessUpdateUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-19T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `updates business settings and writes audit event`() {
        val repository = FakeAdminBusinessMutationRepository().apply {
            business = business()
        }
        val auditLogger = CapturingAdminBusinessAuditLogger()

        val result = useCase(repository, auditLogger).execute(
            command(
                legalName = "Hermes Demo Actualizada S.A.",
                commercialName = "Hermes Demo Actualizado",
                defaultCurrency = "usd",
                timezone = "America/Guayaquil",
            )
        )

        assertEquals("Hermes Demo Actualizada S.A.", result.business.legalName)
        assertEquals("Hermes Demo Actualizado", result.business.commercialName)
        assertEquals("USD", result.business.defaultCurrency)
        assertEquals("America/Guayaquil", result.business.timezone)
        assertEquals(2L, result.business.version)
        assertEquals(1, auditLogger.events.size)
        assertEquals(AdminBusinessAuditAction.BUSINESS_SETTINGS_UPDATED, auditLogger.events.single().action)
        assertEquals("Corrección de datos administrativos", auditLogger.events.single().reason)
    }

    @Test
    fun `rejects update without organization update permission`() {
        val repository = FakeAdminBusinessMutationRepository().apply { business = business() }

        assertFailsWith<DomainRuleViolation> {
            useCase(repository).execute(
                command(
                    permissions = setOf(PermissionCatalog.ORGANIZATION_VIEW),
                    commercialName = "Nuevo nombre",
                )
            )
        }
    }

    @Test
    fun `rejects duplicate country code and tax id`() {
        val repository = FakeAdminBusinessMutationRepository().apply {
            business = business()
            duplicateTaxIds += "EC:1799999999001"
        }

        assertFailsWith<DomainRuleViolation> {
            useCase(repository).execute(command(taxId = "1799999999001"))
        }
    }

    @Test
    fun `rejects update without reason`() {
        val repository = FakeAdminBusinessMutationRepository().apply { business = business() }

        assertFailsWith<DomainRuleViolation> {
            useCase(repository).execute(command(commercialName = "Nuevo nombre", reason = " "))
        }
    }

    @Test
    fun `rejects update with no effective changes`() {
        val repository = FakeAdminBusinessMutationRepository().apply { business = business() }

        assertFailsWith<DomainRuleViolation> {
            useCase(repository).execute(command(commercialName = "Hermes Demo"))
        }
    }

    private fun useCase(
        repository: FakeAdminBusinessMutationRepository,
        auditLogger: AdminBusinessAuditLogger = NoopAdminBusinessAuditLogger,
    ): UpdateAdminBusinessUseCase = UpdateAdminBusinessUseCase(
        readRepository = repository,
        mutationRepository = repository,
        auditLogger = auditLogger,
        clock = clock,
    )

    private fun command(
        permissions: Set<String> = setOf(PermissionCatalog.ORGANIZATION_UPDATE),
        countryCode: String? = null,
        taxId: String? = null,
        legalName: String? = null,
        commercialName: String? = null,
        defaultCurrency: String? = null,
        timezone: String? = null,
        reason: String = "Corrección de datos administrativos",
    ): UpdateAdminBusinessCommand = UpdateAdminBusinessCommand(
        organizationId = "org_1",
        actorUserId = "usr_1",
        actorEffectivePermissions = permissions,
        countryCode = countryCode,
        taxId = taxId,
        legalName = legalName,
        commercialName = commercialName,
        defaultCurrency = defaultCurrency,
        timezone = timezone,
        reason = reason,
    )

    private fun business(): AdminBusinessProfile = AdminBusinessProfile(
        id = "org_1",
        countryCode = "EC",
        taxId = "1790000000001",
        legalName = "Hermes Demo S.A.",
        commercialName = "Hermes Demo",
        status = "active",
        ownerUserId = "usr_1",
        defaultCurrency = null,
        timezone = null,
        createdAt = Instant.parse("2026-05-19T00:00:00Z"),
        updatedAt = Instant.parse("2026-05-19T00:00:00Z"),
        version = 1L,
    )
}

private class FakeAdminBusinessMutationRepository : AdminBusinessRepository, AdminBusinessMutationRepository {
    var business: AdminBusinessProfile? = null
    val duplicateTaxIds: MutableSet<String> = mutableSetOf()

    override fun findBusiness(organizationId: String): AdminBusinessProfile? =
        business?.takeIf { it.id == organizationId }

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> = emptyList()
    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> = emptyList()
    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> = emptyList()
    override fun hasTaxSettings(organizationId: String): Boolean = false
    override fun hasSriSettings(organizationId: String): Boolean = false
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = false

    override fun existsBusinessWithTaxId(
        countryCode: String,
        taxId: String,
        excludeOrganizationId: String,
    ): Boolean = "${countryCode.trim().uppercase()}:${taxId.trim()}" in duplicateTaxIds

    override fun updateBusiness(patch: AdminBusinessUpdatePatch): AdminBusinessProfile {
        val current = business ?: throw DomainRuleViolation("Organization does not exist.")
        val updated = current.copy(
            countryCode = patch.countryCode ?: current.countryCode,
            taxId = patch.taxId ?: current.taxId,
            legalName = patch.legalName ?: current.legalName,
            commercialName = patch.commercialName ?: current.commercialName,
            defaultCurrency = patch.defaultCurrency ?: current.defaultCurrency,
            timezone = patch.timezone ?: current.timezone,
            updatedAt = patch.updatedAt,
            version = current.version + 1,
        )
        business = updated
        return updated
    }
}

private class CapturingAdminBusinessAuditLogger : AdminBusinessAuditLogger {
    val events: MutableList<AdminBusinessAuditEvent> = mutableListOf()
    override fun log(event: AdminBusinessAuditEvent) {
        events += event
    }
}
