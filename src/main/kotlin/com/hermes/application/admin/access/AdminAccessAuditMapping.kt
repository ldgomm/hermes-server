package com.hermes.application.admin.access

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.invitation.Invitation

internal fun AdminUserAccessRecord.toAdminAccessAuditMap(): Map<String, String?> = mapOf(
    "userId" to user.id,
    "email" to user.email,
    "displayName" to user.displayName,
    "phone" to user.phone,
    "userStatus" to user.status.name,
    "blockedAt" to user.blockedAt?.toString(),
    "blockedReason" to user.blockedReason,
    "membershipId" to membership.id,
    "membershipStatus" to membership.status.name,
    "roleIds" to membership.roleIds.sorted().joinToString(","),
    "roleNames" to roles.map { it.name }.sorted().joinToString(","),
    "effectivePermissions" to roles.flatMap { it.permissionKeys }.toSet().sorted().joinToString(","),
    "activeSessionCount" to activeSessionCount.toString(),
    "userVersion" to user.version.toString(),
    "membershipVersion" to membership.version.toString(),
)

internal fun UserCredential.toAdminAccessAuditMap(): Map<String, String?> = mapOf(
    "credentialId" to id,
    "credentialUserId" to userId,
    "credentialStatus" to status.name,
    "mustChangePassword" to mustChangePassword.toString(),
    "temporaryPassword" to temporaryPassword.toString(),
    "failedAttempts" to failedAttempts.toString(),
    "lockedUntil" to lockedUntil?.toString(),
    "lastPasswordChangedAt" to lastPasswordChangedAt?.toString(),
    "credentialVersion" to version.toString(),
)


internal fun Invitation.toAdminAccessAuditMap(maskToken: Boolean = true): Map<String, String?> = mapOf(
    "id" to id,
    "organizationId" to organizationId,
    "email" to email,
    "invitedByUserId" to invitedByUserId,
    "roleIds" to roleIds.sorted().joinToString(","),
    "status" to status.name,
    "tokenHash" to if (maskToken) "***" else tokenHash,
    "createdAt" to createdAt.toString(),
    "expiresAt" to expiresAt.toString(),
    "acceptedAt" to acceptedAt?.toString(),
    "revokedAt" to revokedAt?.toString(),
    "acceptedUserId" to acceptedUserId,
    "version" to version.toString(),
)
