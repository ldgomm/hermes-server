package com.hermes.application.admin.access

import com.hermes.application.auth.InvitationDelivery
import com.hermes.application.auth.NoopInvitationDelivery
import com.hermes.application.auth.SecureTokenGenerator
import com.hermes.application.auth.TokenHasher
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.util.*

class ListAdminUsersUseCase(
    private val repository: AdminAccessRepository,
) {
    fun execute(command: ListAdminUsersCommand): AdminUsersResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_VIEW)
        val organizationId = command.organizationId.required("Organization id")
        return AdminUsersResult(
            repository.listUserAccess(
                organizationId = organizationId,
                query = command.query,
                status = command.status,
                limit = command.limit.coerceIn(1, 250),
            ).map { it.toSummary() })
    }
}

class GetAdminUserUseCase(
    private val repository: AdminAccessRepository,
) {
    fun execute(command: GetAdminUserCommand): AdminUserResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_VIEW)
        val record = repository.findUserAccess(
            organizationId = command.organizationId.required("Organization id"),
            userId = command.userId.required("User id"),
        ) ?: throw DomainRuleViolation("Admin user does not exist in this organization.")
        return AdminUserResult(record.toDetail())
    }
}

class UpdateAdminUserUseCase(
    private val repository: AdminAccessRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
) {
    fun execute(command: UpdateAdminUserCommand): AdminUserResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_CREATE)

        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val userId = command.userId.required("User id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("User update reason")

        if (command.phone != null && command.clearPhone) {
            throw DomainRuleViolation("User phone cannot be set and cleared at the same time.")
        }

        val record = repository.findUserAccess(organizationId, userId)
            ?: throw DomainRuleViolation("Admin user does not exist in this organization.")
        val before = record.toAuditMap()

        var userChanged = false
        val nextDisplayName = command.displayName?.required("Display name") ?: record.user.displayName
        val nextPhone = when {
            command.clearPhone -> null
            command.phone != null -> command.phone.trim().takeIf { it.isNotBlank() }
            else -> record.user.phone
        }

        if (nextDisplayName != record.user.displayName || nextPhone != record.user.phone) {
            repository.updateUser(
                record.user.copy(
                    displayName = nextDisplayName,
                    phone = nextPhone,
                    updatedAt = now,
                    version = record.user.version + 1,
                )
            )
            userChanged = true
        }

        var membershipChanged = false
        val roleIds = command.roleIds
        if (roleIds != null) {
            val normalizedRoleIds = roleIds.map { it.required("Role id") }.toSet()
            if (normalizedRoleIds.isEmpty()) {
                throw DomainRuleViolation("User must have at least one role.")
            }

            val roles = repository.findRolesByIds(normalizedRoleIds)
            if (roles.size != normalizedRoleIds.size) {
                throw DomainRuleViolation("One or more roles do not exist.")
            }
            if (roles.any { it.isPlatformRole }) {
                throw DomainRuleViolation("Organization users cannot receive platform roles.")
            }
            if (roles.any { it.type == RoleType.CUSTOM && it.organizationId != organizationId }) {
                throw DomainRuleViolation("Custom role does not belong to this organization.")
            }

            ensureNotRemovingLastAdmin(
                repository = repository,
                organizationId = organizationId,
                targetUserId = userId,
                currentRoleIds = record.membership.roleIds,
                nextRoles = roles,
            )

            if (normalizedRoleIds != record.membership.roleIds) {
                repository.updateMembership(
                    record.membership.copy(
                        roleIds = normalizedRoleIds,
                        updatedAt = now,
                        version = record.membership.version + 1,
                    )
                )
                membershipChanged = true
            }
        }

        if (!userChanged && !membershipChanged) {
            throw DomainRuleViolation("User update does not contain changes.")
        }

        val updated = repository.findUserAccess(organizationId, userId)
            ?: throw DomainRuleViolation("Updated admin user does not exist.")
        auditLogger.log(
            AdminAccessAuditEvent(
                action = AdminAccessAuditAction.USER_ACCESS_UPDATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = userId,
                targetType = "user_access",
                before = before,
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return AdminUserResult(updated.toDetail())
    }
}

class ListAdminInvitationsUseCase(
    private val repository: AdminAccessRepository,
) {
    fun execute(command: ListAdminInvitationsCommand): AdminInvitationsResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_INVITE)
        val organizationId = command.organizationId.required("Organization id")
        val invitations = repository.listInvitations(
            organizationId = organizationId,
            status = command.status,
            limit = command.limit.coerceIn(1, 250),
        )
        return AdminInvitationsResult(
            invitations.map { invitation ->
                invitation.toSummary(repository.findRolesByIds(invitation.roleIds))
            })
    }
}

class GetAdminInvitationUseCase(
    private val repository: AdminAccessRepository,
) {
    fun execute(command: GetAdminInvitationCommand): AdminInvitationResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_INVITE)
        val invitation = repository.findInvitation(
            organizationId = command.organizationId.required("Organization id"),
            invitationId = command.invitationId.required("Invitation id"),
        ) ?: throw DomainRuleViolation("Invitation does not exist.")
        return AdminInvitationResult(invitation.toSummary(repository.findRolesByIds(invitation.roleIds)))
    }
}

class RevokeAdminInvitationUseCase(
    private val repository: AdminAccessRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
) {
    fun execute(command: RevokeAdminInvitationCommand): AdminInvitationResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_INVITE)
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Invitation revoke reason")
        val organizationId = command.organizationId.required("Organization id")
        val invitation = repository.findInvitation(
            organizationId = organizationId,
            invitationId = command.invitationId.required("Invitation id"),
        ) ?: throw DomainRuleViolation("Invitation does not exist.")

        val now = Instant.now(clock)
        val revoked = invitation.revoke(now)
        repository.updateInvitation(revoked)

        auditLogger.log(
            AdminAccessAuditEvent(
                action = AdminAccessAuditAction.INVITATION_REVOKED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = revoked.id,
                targetType = "invitation",
                before = invitation.toAuditMap(),
                after = revoked.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminInvitationResult(revoked.toSummary(repository.findRolesByIds(revoked.roleIds)))
    }
}

class ResendAdminInvitationUseCase(
    private val repository: AdminAccessRepository,
    private val tokenGenerator: SecureTokenGenerator = SecureTokenGenerator(),
    private val delivery: InvitationDelivery = NoopInvitationDelivery,
    private val resendTtl: java.time.Duration = java.time.Duration.ofDays(7),
    private val clock: Clock = Clock.systemUTC(),
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
) {
    fun execute(command: ResendAdminInvitationCommand): AdminInvitationResendResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_INVITE)
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Invitation resend reason")

        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val invitation = repository.findInvitation(
            organizationId = organizationId,
            invitationId = command.invitationId.required("Invitation id"),
        ) ?: throw DomainRuleViolation("Invitation does not exist.")

        if (invitation.status != InvitationStatus.PENDING) {
            throw DomainRuleViolation("Only pending invitations can be resent.")
        }

        val rawToken = tokenGenerator.generate()
        val refreshed = invitation.copy(
            tokenHash = TokenHasher.sha256(rawToken),
            expiresAt = now.plus(resendTtl),
            version = invitation.version + 1,
        )
        repository.updateInvitation(refreshed)

        val invitationUrl = delivery.buildInvitationUrl(rawToken)
        delivery.deliverInvitation(refreshed.email, rawToken, invitationUrl)

        auditLogger.log(
            AdminAccessAuditEvent(
                action = AdminAccessAuditAction.INVITATION_RESENT,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = refreshed.id,
                targetType = "invitation",
                before = invitation.toAuditMap(maskToken = true),
                after = refreshed.toAuditMap(maskToken = true),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminInvitationResendResult(
            invitation = refreshed.toSummary(repository.findRolesByIds(refreshed.roleIds)),
            rawInvitationToken = rawToken,
            invitationUrl = invitationUrl,
        )
    }
}

class ListAdminRolesUseCase(
    private val repository: AdminAccessRepository,
) {
    fun execute(command: ListAdminRolesCommand): AdminRolesResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_ROLES_VIEW)
        val organizationId = command.organizationId.required("Organization id")
        return AdminRolesResult(
            repository.listRoles(
                organizationId = organizationId,
                includeSystemTemplates = command.includeSystemTemplates,
            ).sortedWith(compareBy({ it.type.name }, { it.name })).map { it.toSummary() })
    }
}

class GetAdminRoleUseCase(
    private val repository: AdminAccessRepository,
) {
    fun execute(command: GetAdminRoleCommand): AdminRoleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_ROLES_VIEW)
        val role = repository.findRole(
            organizationId = command.organizationId.required("Organization id"),
            roleId = command.roleId.required("Role id"),
        ) ?: throw DomainRuleViolation("Role does not exist.")
        return AdminRoleResult(role.toSummary())
    }
}

class CreateAdminRoleUseCase(
    private val repository: AdminAccessRepository,
    private val idGenerator: AdminAccessIdGenerator = UuidAdminAccessIdGenerator(),
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateAdminRoleCommand): AdminRoleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_ROLES_MANAGE)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Role creation reason")
        val now = Instant.now(clock)

        val code = command.code.normalizedRoleCode()
        if (repository.existsRoleCode(organizationId, code)) {
            throw DomainRuleViolation("Role code already exists: $code.")
        }

        val permissionKeys = command.permissionKeys.normalizedPermissionKeys()
        val role = RoleDefinition(
            id = idGenerator.newId("role"),
            code = code,
            organizationId = organizationId,
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = command.name.required("Role name"),
            description = command.description.required("Role description"),
            permissionKeys = permissionKeys,
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        )

        repository.createRole(role)
        auditLogger.log(
            AdminAccessAuditEvent(
                action = AdminAccessAuditAction.ROLE_CREATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = role.id,
                targetType = "role",
                after = role.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return AdminRoleResult(role.toSummary())
    }
}

class UpdateAdminRoleUseCase(
    private val repository: AdminAccessRepository,
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UpdateAdminRoleCommand): AdminRoleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_ROLES_MANAGE)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Role update reason")
        val now = Instant.now(clock)

        val current = repository.findRole(organizationId, command.roleId.required("Role id"))
            ?: throw DomainRuleViolation("Role does not exist.")
        current.assertEditableCustomRole(organizationId)

        val nextPermissionKeys = command.permissionKeys?.normalizedPermissionKeys() ?: current.permissionKeys
        if (current.status == RoleStatus.ACTIVE && current.grantsAdminAccess() && !nextPermissionKeys.grantsAdminAccess()) {
            ensureRoleChangeDoesNotRemoveLastAdmin(repository, organizationId, current)
        }

        val next = current.copy(
            name = command.name?.required("Role name") ?: current.name,
            description = command.description?.required("Role description") ?: current.description,
            permissionKeys = nextPermissionKeys,
            schemaVersion = current.schemaVersion + 1,
        )

        if (next.name == current.name && next.description == current.description && next.permissionKeys == current.permissionKeys) {
            throw DomainRuleViolation("Role update does not contain changes.")
        }

        repository.updateRole(next)
        auditLogger.log(
            AdminAccessAuditEvent(
                action = AdminAccessAuditAction.ROLE_UPDATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = next.id,
                targetType = "role",
                before = current.toAuditMap(),
                after = next.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return AdminRoleResult(next.toSummary())
    }
}

class ChangeAdminRoleStatusUseCase(
    private val repository: AdminAccessRepository,
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun activate(command: ChangeAdminRoleStatusCommand): AdminRoleResult =
        change(command.copy(targetStatus = RoleStatus.ACTIVE.name), AdminAccessAuditAction.ROLE_ACTIVATED)

    fun deactivate(command: ChangeAdminRoleStatusCommand): AdminRoleResult =
        change(command.copy(targetStatus = RoleStatus.INACTIVE.name), AdminAccessAuditAction.ROLE_DEACTIVATED)

    private fun change(command: ChangeAdminRoleStatusCommand, action: AdminAccessAuditAction): AdminRoleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_ROLES_MANAGE)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Role status change reason")
        val now = Instant.now(clock)

        val current = repository.findRole(organizationId, command.roleId.required("Role id"))
            ?: throw DomainRuleViolation("Role does not exist.")
        current.assertEditableCustomRole(organizationId)

        val targetStatus = command.targetStatus.normalizedRoleStatus()
        if (current.status == targetStatus) {
            throw DomainRuleViolation("Role is already ${targetStatus.name}.")
        }

        if (targetStatus == RoleStatus.INACTIVE && current.status == RoleStatus.ACTIVE && current.grantsAdminAccess()) {
            ensureRoleChangeDoesNotRemoveLastAdmin(repository, organizationId, current)
        }

        val next = current.copy(status = targetStatus, schemaVersion = current.schemaVersion + 1)
        repository.updateRole(next)
        auditLogger.log(
            AdminAccessAuditEvent(
                action = action,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = next.id,
                targetType = "role",
                before = current.toAuditMap(),
                after = next.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return AdminRoleResult(next.toSummary())
    }
}

class ListAdminPermissionsUseCase(
    private val repository: AdminAccessRepository,
) {
    fun execute(command: ListAdminPermissionsCommand): AdminPermissionsResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_ROLES_VIEW)
        command.organizationId.required("Organization id")
        return AdminPermissionsResult(
            repository.listPermissionDefinitions(command.includeReserved)
                .sortedWith(compareBy({ it.category.name }, { it.code })).map { it.toSummary() })
    }
}

fun interface AdminAccessIdGenerator {
    fun newId(prefix: String): String
}

class UuidAdminAccessIdGenerator : AdminAccessIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}

internal fun String.required(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")

private fun String.normalizedRoleCode(): String =
    required("Role code").lowercase().replace(Regex("[^a-z0-9_]+"), "_").replace(Regex("_+"), "_").trim('_')
        .takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("Role code is invalid.")

private fun Set<String>.normalizedPermissionKeys(): Set<String> {
    if (isEmpty()) throw DomainRuleViolation("Role requires at least one permission.")
    val normalized = map { it.required("Permission key") }.toSet()
    val unknown = normalized.filterNot { it in PermissionCatalog.known }
    if (unknown.isNotEmpty()) {
        throw DomainRuleViolation("Unknown permission keys: ${unknown.sorted().joinToString()}.")
    }
    if (PermissionCatalog.ALL in normalized) {
        throw DomainRuleViolation("Organization custom roles cannot use wildcard permission.")
    }
    return normalized
}

private fun String.normalizedRoleStatus(): RoleStatus =
    runCatching { RoleStatus.valueOf(required("Role status").uppercase()) }.getOrElse { throw DomainRuleViolation("Unsupported role status: $this.") }

private fun RoleDefinition.assertEditableCustomRole(organizationId: String) {
    if (type != RoleType.CUSTOM || this.organizationId != organizationId) {
        throw DomainRuleViolation("Only organization custom roles can be edited.")
    }
    if (!editable || critical || systemRole) {
        throw DomainRuleViolation("Role is not editable.")
    }
    if (status == RoleStatus.ARCHIVED) {
        throw DomainRuleViolation("Archived role cannot be edited.")
    }
}

private val adminPermissionKeys = setOf(
    PermissionCatalog.ALL,
    PermissionCatalog.CREDENTIALS_USERS_CREATE,
    PermissionCatalog.CREDENTIALS_USERS_INVITE,
    PermissionCatalog.CREDENTIALS_ROLES_MANAGE,
    PermissionCatalog.ORGANIZATION_UPDATE,
)

private fun ensureNotRemovingLastAdmin(
    repository: AdminAccessRepository,
    organizationId: String,
    targetUserId: String,
    currentRoleIds: Set<String>,
    nextRoles: List<RoleDefinition>,
) {
    val currentRoles = repository.findRolesByIds(currentRoleIds)
    val currentlyAdmin = currentRoles.any { it.grantsAdminAccess() }
    val nextAdmin = nextRoles.any { it.grantsAdminAccess() }

    if (currentlyAdmin && !nextAdmin) {
        val remainingAdmins = repository.countActiveAdminMemberships(
            organizationId = organizationId,
            excludingUserId = targetUserId,
            adminPermissionKeys = adminPermissionKeys,
        )
        if (remainingAdmins == 0) {
            throw DomainRuleViolation("Cannot remove the last active administrator from the organization.")
        }
    }
}

private fun ensureRoleChangeDoesNotRemoveLastAdmin(
    repository: AdminAccessRepository,
    organizationId: String,
    roleBeingChanged: RoleDefinition,
) {
    val remainingAdminUsers = repository.listUserAccess(
        organizationId = organizationId,
        status = MembershipStatus.ACTIVE.name,
        limit = 250,
    ).count { record ->
        record.membership.status == MembershipStatus.ACTIVE && record.roles.filterNot { it.id == roleBeingChanged.id }
            .any { it.status == RoleStatus.ACTIVE && it.grantsAdminAccess() }
    }

    if (remainingAdminUsers == 0) {
        throw DomainRuleViolation("Cannot change this role because it would remove the last active administrator from the organization.")
    }
}

private fun RoleDefinition.grantsAdminAccess(): Boolean =
    permissionKeys.grantsAdminAccess() && status == RoleStatus.ACTIVE

private fun Set<String>.grantsAdminAccess(): Boolean = any { it in adminPermissionKeys }

private fun AdminUserAccessRecord.toAuditMap(): Map<String, String?> = mapOf(
    "userId" to user.id,
    "email" to user.email,
    "displayName" to user.displayName,
    "phone" to user.phone,
    "userStatus" to user.status.name,
    "membershipId" to membership.id,
    "membershipStatus" to membership.status.name,
    "roleIds" to membership.roleIds.sorted().joinToString(","),
    "roleNames" to roles.map { it.name }.sorted().joinToString(","),
    "activeSessionCount" to activeSessionCount.toString(),
    "version" to user.version.toString(),
)

private fun Invitation.toAuditMap(maskToken: Boolean = false): Map<String, String?> = mapOf(
    "id" to id,
    "organizationId" to organizationId,
    "email" to email,
    "invitedByUserId" to invitedByUserId,
    "roleIds" to roleIds.sorted().joinToString(","),
    "status" to status.name,
    "tokenHash" to if (maskToken) "***" else tokenHash,
    "expiresAt" to expiresAt.toString(),
    "acceptedAt" to acceptedAt?.toString(),
    "revokedAt" to revokedAt?.toString(),
    "acceptedUserId" to acceptedUserId,
    "version" to version.toString(),
)

private fun RoleDefinition.toAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "code" to code,
    "organizationId" to organizationId,
    "scope" to scope.name,
    "type" to type.name,
    "name" to name,
    "description" to description,
    "permissionKeys" to permissionKeys.sorted().joinToString(","),
    "systemRole" to systemRole.toString(),
    "critical" to critical.toString(),
    "editable" to editable.toString(),
    "status" to status.name,
    "schemaVersion" to schemaVersion.toString(),
)
