package com.hermes.application.admin.support

import com.hermes.application.auth.AuthorizationPolicy
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation

class SearchAdminAuditLogsUseCase(private val repository: AdminSupportQueryRepository) {
    fun execute(command: SearchAdminAuditLogsCommand): AdminAuditLogsResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.limit.requireLimit(max = 500)
        command.requireValidRange()
        AuthorizationPolicy.requirePermission(command.actorEffectivePermissions, PermissionCatalog.AUDIT_VIEW)
        return AdminAuditLogsResult(repository.searchAuditLogs(command))
    }
}

class GetAdminAuditTimelineUseCase(private val repository: AdminSupportQueryRepository) {
    fun execute(command: GetAdminAuditTimelineCommand): AdminAuditTimelineResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.limit.requireLimit(max = 500)
        command.requireValidRange()
        AuthorizationPolicy.requirePermission(command.actorEffectivePermissions, PermissionCatalog.AUDIT_VIEW)
        return AdminAuditTimelineResult(repository.auditTimeline(command))
    }
}

class GetAdminSupportDiagnosticsUseCase(private val repository: AdminSupportQueryRepository) {
    fun execute(command: GetAdminSupportDiagnosticsCommand): AdminSupportDiagnosticsResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.ORGANIZATION_VIEW),
        )
        return AdminSupportDiagnosticsResult(repository.diagnostics(command))
    }
}

class GetAdminSupportPermissionsUseCase(private val repository: AdminSupportQueryRepository) {
    fun execute(command: GetAdminSupportPermissionsCommand): AdminSupportPermissionsResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.CREDENTIALS_ROLES_VIEW),
        )
        return AdminSupportPermissionsResult(repository.permissions(command))
    }
}

class GetAdminSupportModulesUseCase(private val repository: AdminSupportQueryRepository) {
    fun execute(command: GetAdminSupportModulesCommand): AdminSupportModulesResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.ORGANIZATION_VIEW),
        )
        return AdminSupportModulesResult(repository.modules(command))
    }
}

internal fun String.requireNotBlank(label: String): String = trim().takeIf { it.isNotBlank() }
    ?: throw DomainRuleViolation("$label cannot be blank.")

private fun Int.requireLimit(max: Int) {
    if (this !in 1..max) throw DomainRuleViolation("Limit must be between 1 and $max.")
}

private fun SearchAdminAuditLogsCommand.requireValidRange() {
    if (from != null && to != null && !from.isBefore(to)) {
        throw DomainRuleViolation("Audit query 'from' must be before 'to'.")
    }
}

private fun GetAdminAuditTimelineCommand.requireValidRange() {
    if (from != null && to != null && !from.isBefore(to)) {
        throw DomainRuleViolation("Timeline query 'from' must be before 'to'.")
    }
}
