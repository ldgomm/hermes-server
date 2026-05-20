package com.hermes.application.admin.access

import java.time.Instant

/**
 * Phase 13B hardening audit port.
 *
 * This logger is intentionally independent from CredentialAuditLogger because it
 * audits admin access configuration changes: user profile/roles, invitations and
 * role definitions. A Mongo implementation can later persist these events into
 * audit_logs/domain_events without changing use cases.
 */
enum class AdminAccessAuditAction {
    TEMPORARY_USER_CREATED,
    USER_PASSWORD_RESET,
    USER_ACCESS_UPDATED,
    USER_BLOCKED,
    USER_UNBLOCKED,
    USER_SESSIONS_REVOKED,
    INVITATION_RESENT,
    INVITATION_REVOKED,
    ROLE_CREATED,
    ROLE_UPDATED,
    ROLE_ACTIVATED,
    ROLE_DEACTIVATED,
}

data class AdminAccessAuditEvent(
    val action: AdminAccessAuditAction,
    val organizationId: String,
    val actorUserId: String,
    val targetId: String,
    val targetType: String,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String,
    val createdAt: Instant,
)

interface AdminAccessAuditLogger {
    fun log(event: AdminAccessAuditEvent)
}

object NoopAdminAccessAuditLogger : AdminAccessAuditLogger {
    override fun log(event: AdminAccessAuditEvent) = Unit
}
