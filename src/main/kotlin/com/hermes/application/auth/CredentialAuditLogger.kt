package com.hermes.application.auth

import java.time.Instant

enum class CredentialAuditAction {
    USER_CREATED,
    CREDENTIAL_CREATED,
    ORGANIZATION_CREATED,
    MEMBERSHIP_CREATED,
    ORGANIZATION_OWNER_ASSIGNED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    REFRESH_TOKEN_ROTATED,
    REFRESH_TOKEN_REUSE_DETECTED,
    SESSION_REVOKED,
    ALL_SESSIONS_REVOKED,
    USER_SESSIONS_REVOKED_BY_ADMIN,
}

data class CredentialAuditEvent(
    val action: CredentialAuditAction,
    val actorUserId: String?,
    val targetUserId: String?,
    val organizationId: String?,
    val sessionId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val message: String? = null,
    val createdAt: Instant,
)

interface CredentialAuditLogger {
    fun log(event: CredentialAuditEvent)
}

object NoopCredentialAuditLogger : CredentialAuditLogger {
    override fun log(event: CredentialAuditEvent) = Unit
}
