package com.hermes.application.admin.access

import com.hermes.domain.invitation.Invitation
import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.role.RoleDefinition

internal fun AdminUserAccessRecord.toSummary(): AdminUserAccessSummary = AdminUserAccessSummary(
    id = user.id,
    email = user.email,
    displayName = user.displayName,
    phone = user.phone,
    status = user.status.name,
    membershipId = membership.id,
    membershipStatus = membership.status.name,
    roleIds = membership.roleIds,
    roleNames = roles.sortedBy { it.name }.map { it.name },
    activeSessionCount = activeSessionCount,
    invitedBy = membership.invitedBy,
    acceptedAt = membership.acceptedAt,
    createdAt = user.createdAt,
    updatedAt = user.updatedAt,
)

internal fun AdminUserAccessRecord.toDetail(): AdminUserAccessDetail = AdminUserAccessDetail(
    id = user.id,
    email = user.email,
    displayName = user.displayName,
    phone = user.phone,
    status = user.status.name,
    blockedAt = user.blockedAt,
    blockedReason = user.blockedReason,
    membershipId = membership.id,
    membershipStatus = membership.status.name,
    roles = roles.sortedBy { it.name }.map { it.toSummary() },
    effectivePermissions = roles.flatMap { it.permissionKeys }.toSet(),
    activeSessionCount = activeSessionCount,
    invitedBy = membership.invitedBy,
    acceptedAt = membership.acceptedAt,
    createdAt = user.createdAt,
    updatedAt = user.updatedAt,
    version = user.version,
)

internal fun Invitation.toSummary(roles: List<RoleDefinition>): AdminInvitationSummary = AdminInvitationSummary(
    id = id,
    organizationId = organizationId,
    email = email,
    invitedByUserId = invitedByUserId,
    roleIds = roleIds,
    roleNames = roles.sortedBy { it.name }.map { it.name },
    status = status.name,
    createdAt = createdAt,
    expiresAt = expiresAt,
    acceptedAt = acceptedAt,
    revokedAt = revokedAt,
    acceptedUserId = acceptedUserId,
    version = version,
)

internal fun RoleDefinition.toSummary(): AdminRoleSummary = AdminRoleSummary(
    id = id,
    code = code,
    organizationId = organizationId,
    scope = scope.name,
    type = type.name,
    name = name,
    description = description,
    permissionKeys = permissionKeys,
    systemRole = systemRole,
    critical = critical,
    editable = editable,
    status = status.name,
    schemaVersion = schemaVersion,
)

internal fun PermissionDefinition.toSummary(): AdminPermissionSummary = AdminPermissionSummary(
    code = code,
    name = name,
    description = description,
    category = category.name,
    scope = scope.name,
    riskLevel = riskLevel.name,
    status = status.name,
    systemManaged = systemManaged,
    requiresAudit = requiresAudit,
    requiresReason = requiresReason,
    requiresStepUp = requiresStepUp,
    featureFlag = featureFlag,
)
