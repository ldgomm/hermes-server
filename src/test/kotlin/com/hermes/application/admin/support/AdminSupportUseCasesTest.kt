package com.hermes.application.admin.support

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdminSupportUseCasesTest {
    private val now = Instant.parse("2026-05-20T00:00:00Z")

    @Test
    fun `audit logs require audit permission`() {
        val useCase = SearchAdminAuditLogsUseCase(InMemoryAdminSupportQueryRepository(now))

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                SearchAdminAuditLogsCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.ORGANIZATION_VIEW),
                ),
            )
        }
    }

    @Test
    fun `audit logs return redacted records from repository`() {
        val useCase = SearchAdminAuditLogsUseCase(InMemoryAdminSupportQueryRepository(now))

        val result = useCase.execute(
            SearchAdminAuditLogsCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.AUDIT_VIEW),
            ),
        )

        assertEquals(1, result.logs.size)
        assertEquals("USER_BLOCKED", result.logs.first().action)
        assertEquals("***", result.logs.first().after.getValue("tokenHash"))
    }

    @Test
    fun `timeline validates range`() {
        val useCase = GetAdminAuditTimelineUseCase(InMemoryAdminSupportQueryRepository(now))

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                GetAdminAuditTimelineCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.AUDIT_VIEW),
                    from = Instant.parse("2026-05-21T00:00:00Z"),
                    to = Instant.parse("2026-05-20T00:00:00Z"),
                ),
            )
        }
    }

    @Test
    fun `support diagnostics can be read by organization viewer`() {
        val useCase = GetAdminSupportDiagnosticsUseCase(InMemoryAdminSupportQueryRepository(now))

        val result = useCase.execute(
            GetAdminSupportDiagnosticsCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.ORGANIZATION_VIEW),
            ),
        )

        assertEquals("ok", result.report.status)
        assertTrue(result.report.checks.any { it.code == "secret_redaction_enabled" })
    }

    @Test
    fun `permission diagnostics flags risky permissions`() {
        val useCase = GetAdminSupportPermissionsUseCase(InMemoryAdminSupportQueryRepository(now))

        val result = useCase.execute(
            GetAdminSupportPermissionsCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.TAX_MANAGE),
            ),
        )

        assertTrue(result.report.canViewAudit)
        assertTrue(PermissionCatalog.TAX_MANAGE in result.report.riskyPermissions)
    }

    @Test
    fun `module diagnostics reports missing permissions`() {
        val useCase = GetAdminSupportModulesUseCase(InMemoryAdminSupportQueryRepository(now))

        val result = useCase.execute(
            GetAdminSupportModulesCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.AUDIT_VIEW),
            ),
        )

        assertTrue(result.report.modules.any { it.code == "audit_support" && it.enabled })
        assertTrue(result.report.modules.any { it.code == "tax_admin" && it.missingPermissions.isNotEmpty() })
    }

    private class InMemoryAdminSupportQueryRepository(
        private val now: Instant,
    ) : AdminSupportQueryRepository {
        override fun searchAuditLogs(command: SearchAdminAuditLogsCommand): List<AdminAuditLogRecord> = listOf(
            AdminAuditLogRecord(
                id = "audit_1",
                organizationId = command.organizationId,
                source = "admin_access",
                surface = "users",
                action = "USER_BLOCKED",
                actorUserId = "usr_admin",
                targetType = "user",
                targetId = "usr_target",
                reason = "Security review",
                message = null,
                severity = "warning",
                correlationId = null,
                after = mapOf("tokenHash" to "***"),
                createdAt = now,
            ),
        )

        override fun auditTimeline(command: GetAdminAuditTimelineCommand): List<AdminAuditTimelineItem> = listOf(
            AdminAuditTimelineItem(
                id = "audit_1",
                organizationId = command.organizationId,
                occurredAt = now,
                source = "admin_access",
                surface = "users",
                title = "User blocked",
                description = "Actor usr_admin · user usr_target",
                actorUserId = "usr_admin",
                targetType = "user",
                targetId = "usr_target",
                severity = "warning",
                reason = "Security review",
            ),
        )

        override fun diagnostics(command: GetAdminSupportDiagnosticsCommand): AdminSupportDiagnosticsReport = AdminSupportDiagnosticsReport(
            organizationId = command.organizationId,
            generatedAt = now,
            status = "ok",
            checks = listOf(
                AdminSupportDiagnosticCheck("secret_redaction_enabled", "ok", "Redaction enabled."),
            ),
            counters = listOf(AdminSupportCounter("audit_logs", "Audit logs", 1)),
            warnings = emptyList(),
        )

        override fun permissions(command: GetAdminSupportPermissionsCommand): AdminSupportPermissionsReport {
            val permissions = command.actorEffectivePermissions.sorted()
            return AdminSupportPermissionsReport(
                organizationId = command.organizationId,
                actorUserId = command.actorUserId,
                generatedAt = now,
                hasWildcard = PermissionCatalog.ALL in command.actorEffectivePermissions,
                permissionCount = permissions.size,
                permissions = permissions,
                riskyPermissions = permissions.filter { it.contains("manage") || it.contains("tax") },
                missingRecommendedAdminPermissions = emptyList(),
                canViewAudit = PermissionCatalog.AUDIT_VIEW in command.actorEffectivePermissions,
                canViewSupportDiagnostics = true,
            )
        }

        override fun modules(command: GetAdminSupportModulesCommand): AdminSupportModulesReport = AdminSupportModulesReport(
            organizationId = command.organizationId,
            actorUserId = command.actorUserId,
            generatedAt = now,
            modules = listOf(
                AdminSupportModuleStatus(
                    code = "audit_support",
                    name = "Global Audit & Support API",
                    phase = "13F",
                    status = "available",
                    enabled = true,
                    requiredPermissions = listOf(PermissionCatalog.AUDIT_VIEW),
                    missingPermissions = emptyList(),
                    dependencies = listOf("audit_logs"),
                ),
                AdminSupportModuleStatus(
                    code = "tax_admin",
                    name = "Tax Admin API",
                    phase = "13D",
                    status = "restricted",
                    enabled = false,
                    requiredPermissions = listOf(PermissionCatalog.TAX_SETTINGS_VIEW),
                    missingPermissions = listOf(PermissionCatalog.TAX_SETTINGS_VIEW),
                    dependencies = listOf("tax_profiles"),
                ),
            ),
        )
    }
}
