package com.hermes.application.admin.support

import java.time.Instant

// -----------------------------------------------------------------------------
// Commands
// -----------------------------------------------------------------------------

data class SearchAdminAuditLogsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val sources: Set<String> = emptySet(),
    val surfaces: Set<String> = emptySet(),
    val actions: Set<String> = emptySet(),
    val severities: Set<String> = emptySet(),
    val auditedActorUserId: String? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

data class GetAdminAuditTimelineCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val auditedActorUserId: String? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

data class GetAdminSupportDiagnosticsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class GetAdminSupportPermissionsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class GetAdminSupportModulesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

// -----------------------------------------------------------------------------
// Results / read models
// -----------------------------------------------------------------------------

data class AdminAuditLogsResult(
    val logs: List<AdminAuditLogRecord>,
)

data class AdminAuditTimelineResult(
    val items: List<AdminAuditTimelineItem>,
)

data class AdminSupportDiagnosticsResult(
    val report: AdminSupportDiagnosticsReport,
)

data class AdminSupportPermissionsResult(
    val report: AdminSupportPermissionsReport,
)

data class AdminSupportModulesResult(
    val report: AdminSupportModulesReport,
)

data class AdminAuditLogRecord(
    val id: String,
    val organizationId: String,
    val source: String,
    val surface: String,
    val action: String,
    val actorUserId: String?,
    val targetType: String?,
    val targetId: String?,
    val reason: String?,
    val message: String?,
    val severity: String,
    val correlationId: String?,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val metadata: Map<String, String?> = emptyMap(),
    val createdAt: Instant,
)

data class AdminAuditTimelineItem(
    val id: String,
    val organizationId: String,
    val occurredAt: Instant,
    val source: String,
    val surface: String,
    val title: String,
    val description: String,
    val actorUserId: String?,
    val targetType: String?,
    val targetId: String?,
    val severity: String,
    val reason: String?,
    val metadata: Map<String, String?> = emptyMap(),
)

data class AdminSupportDiagnosticsReport(
    val organizationId: String,
    val generatedAt: Instant,
    val status: String,
    val checks: List<AdminSupportDiagnosticCheck>,
    val counters: List<AdminSupportCounter>,
    val warnings: List<String>,
)

data class AdminSupportDiagnosticCheck(
    val code: String,
    val status: String,
    val message: String,
    val actionHint: String? = null,
)

data class AdminSupportCounter(
    val code: String,
    val label: String,
    val value: Long,
)

data class AdminSupportPermissionsReport(
    val organizationId: String,
    val actorUserId: String,
    val generatedAt: Instant,
    val hasWildcard: Boolean,
    val permissionCount: Int,
    val permissions: List<String>,
    val riskyPermissions: List<String>,
    val missingRecommendedAdminPermissions: List<String>,
    val canViewAudit: Boolean,
    val canViewSupportDiagnostics: Boolean,
)

data class AdminSupportModulesReport(
    val organizationId: String,
    val actorUserId: String,
    val generatedAt: Instant,
    val modules: List<AdminSupportModuleStatus>,
)

data class AdminSupportModuleStatus(
    val code: String,
    val name: String,
    val phase: String,
    val status: String,
    val enabled: Boolean,
    val requiredPermissions: List<String>,
    val missingPermissions: List<String>,
    val dependencies: List<String>,
    val notes: List<String> = emptyList(),
)
