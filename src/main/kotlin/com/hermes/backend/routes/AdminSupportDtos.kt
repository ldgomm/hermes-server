package com.hermes.backend.routes

import com.hermes.application.admin.support.AdminAuditLogRecord
import com.hermes.application.admin.support.AdminAuditLogsResult
import com.hermes.application.admin.support.AdminAuditTimelineItem
import com.hermes.application.admin.support.AdminAuditTimelineResult
import com.hermes.application.admin.support.AdminSupportCounter
import com.hermes.application.admin.support.AdminSupportDiagnosticCheck
import com.hermes.application.admin.support.AdminSupportDiagnosticsReport
import com.hermes.application.admin.support.AdminSupportDiagnosticsResult
import com.hermes.application.admin.support.AdminSupportModuleStatus
import com.hermes.application.admin.support.AdminSupportModulesReport
import com.hermes.application.admin.support.AdminSupportModulesResult
import com.hermes.application.admin.support.AdminSupportPermissionsReport
import com.hermes.application.admin.support.AdminSupportPermissionsResult
import kotlinx.serialization.Serializable

@Serializable
data class AdminAuditLogsResponse(val logs: List<AdminAuditLogRecordResponse>)

@Serializable
data class AdminAuditTimelineResponse(val items: List<AdminAuditTimelineItemResponse>)

@Serializable
data class AdminSupportDiagnosticsResponse(val report: AdminSupportDiagnosticsReportResponse)

@Serializable
data class AdminSupportPermissionsResponse(val report: AdminSupportPermissionsReportResponse)

@Serializable
data class AdminSupportModulesResponse(val report: AdminSupportModulesReportResponse)

@Serializable
data class AdminAuditLogRecordResponse(
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
    val before: Map<String, String?>,
    val after: Map<String, String?>,
    val metadata: Map<String, String?>,
    val createdAt: String,
)

@Serializable
data class AdminAuditTimelineItemResponse(
    val id: String,
    val organizationId: String,
    val occurredAt: String,
    val source: String,
    val surface: String,
    val title: String,
    val description: String,
    val actorUserId: String?,
    val targetType: String?,
    val targetId: String?,
    val severity: String,
    val reason: String?,
    val metadata: Map<String, String?>,
)

@Serializable
data class AdminSupportDiagnosticsReportResponse(
    val organizationId: String,
    val generatedAt: String,
    val status: String,
    val checks: List<AdminSupportDiagnosticCheckResponse>,
    val counters: List<AdminSupportCounterResponse>,
    val warnings: List<String>,
)

@Serializable
data class AdminSupportDiagnosticCheckResponse(
    val code: String,
    val status: String,
    val message: String,
    val actionHint: String?,
)

@Serializable
data class AdminSupportCounterResponse(
    val code: String,
    val label: String,
    val value: Long,
)

@Serializable
data class AdminSupportPermissionsReportResponse(
    val organizationId: String,
    val actorUserId: String,
    val generatedAt: String,
    val hasWildcard: Boolean,
    val permissionCount: Int,
    val permissions: List<String>,
    val riskyPermissions: List<String>,
    val missingRecommendedAdminPermissions: List<String>,
    val canViewAudit: Boolean,
    val canViewSupportDiagnostics: Boolean,
)

@Serializable
data class AdminSupportModulesReportResponse(
    val organizationId: String,
    val actorUserId: String,
    val generatedAt: String,
    val modules: List<AdminSupportModuleStatusResponse>,
)

@Serializable
data class AdminSupportModuleStatusResponse(
    val code: String,
    val name: String,
    val phase: String,
    val status: String,
    val enabled: Boolean,
    val requiredPermissions: List<String>,
    val missingPermissions: List<String>,
    val dependencies: List<String>,
    val notes: List<String>,
)

fun AdminAuditLogsResult.toResponse(): AdminAuditLogsResponse = AdminAuditLogsResponse(logs.map { it.toResponse() })
fun AdminAuditTimelineResult.toResponse(): AdminAuditTimelineResponse = AdminAuditTimelineResponse(items.map { it.toResponse() })
fun AdminSupportDiagnosticsResult.toResponse(): AdminSupportDiagnosticsResponse = AdminSupportDiagnosticsResponse(report.toResponse())
fun AdminSupportPermissionsResult.toResponse(): AdminSupportPermissionsResponse = AdminSupportPermissionsResponse(report.toResponse())
fun AdminSupportModulesResult.toResponse(): AdminSupportModulesResponse = AdminSupportModulesResponse(report.toResponse())

private fun AdminAuditLogRecord.toResponse(): AdminAuditLogRecordResponse = AdminAuditLogRecordResponse(
    id = id,
    organizationId = organizationId,
    source = source,
    surface = surface,
    action = action,
    actorUserId = actorUserId,
    targetType = targetType,
    targetId = targetId,
    reason = reason,
    message = message,
    severity = severity,
    correlationId = correlationId,
    before = before,
    after = after,
    metadata = metadata,
    createdAt = createdAt.toString(),
)

private fun AdminAuditTimelineItem.toResponse(): AdminAuditTimelineItemResponse = AdminAuditTimelineItemResponse(
    id = id,
    organizationId = organizationId,
    occurredAt = occurredAt.toString(),
    source = source,
    surface = surface,
    title = title,
    description = description,
    actorUserId = actorUserId,
    targetType = targetType,
    targetId = targetId,
    severity = severity,
    reason = reason,
    metadata = metadata,
)

private fun AdminSupportDiagnosticsReport.toResponse(): AdminSupportDiagnosticsReportResponse = AdminSupportDiagnosticsReportResponse(
    organizationId = organizationId,
    generatedAt = generatedAt.toString(),
    status = status,
    checks = checks.map { it.toResponse() },
    counters = counters.map { it.toResponse() },
    warnings = warnings,
)

private fun AdminSupportDiagnosticCheck.toResponse(): AdminSupportDiagnosticCheckResponse = AdminSupportDiagnosticCheckResponse(
    code = code,
    status = status,
    message = message,
    actionHint = actionHint,
)

private fun AdminSupportCounter.toResponse(): AdminSupportCounterResponse = AdminSupportCounterResponse(
    code = code,
    label = label,
    value = value,
)

private fun AdminSupportPermissionsReport.toResponse(): AdminSupportPermissionsReportResponse = AdminSupportPermissionsReportResponse(
    organizationId = organizationId,
    actorUserId = actorUserId,
    generatedAt = generatedAt.toString(),
    hasWildcard = hasWildcard,
    permissionCount = permissionCount,
    permissions = permissions,
    riskyPermissions = riskyPermissions,
    missingRecommendedAdminPermissions = missingRecommendedAdminPermissions,
    canViewAudit = canViewAudit,
    canViewSupportDiagnostics = canViewSupportDiagnostics,
)

private fun AdminSupportModulesReport.toResponse(): AdminSupportModulesReportResponse = AdminSupportModulesReportResponse(
    organizationId = organizationId,
    actorUserId = actorUserId,
    generatedAt = generatedAt.toString(),
    modules = modules.map { it.toResponse() },
)

private fun AdminSupportModuleStatus.toResponse(): AdminSupportModuleStatusResponse = AdminSupportModuleStatusResponse(
    code = code,
    name = name,
    phase = phase,
    status = status,
    enabled = enabled,
    requiredPermissions = requiredPermissions,
    missingPermissions = missingPermissions,
    dependencies = dependencies,
    notes = notes,
)
