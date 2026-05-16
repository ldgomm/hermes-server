package com.hermes.application.auth

import java.time.Instant

enum class CredentialAuditAction {
    USER_CREATED,
    USER_CREATED_BY_ADMIN,
    CREDENTIAL_CREATED,
    ORGANIZATION_CREATED,
    MEMBERSHIP_CREATED,
    ORGANIZATION_OWNER_ASSIGNED,

    USER_INVITED,
    INVITATION_CREATED,
    INVITATION_ACCEPTED,
    INVITATION_REVOKED,

    TEMPORARY_PASSWORD_CREATED,
    PASSWORD_CHANGED,
    PASSWORD_CHANGE_REQUIRED_COMPLETED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,

    USER_BLOCKED,
    USER_UNBLOCKED,

    ROLE_ASSIGNED,
    ROLE_REMOVED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,

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
