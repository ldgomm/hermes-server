package com.hermes.application.admin.access

import com.hermes.application.auth.InviteUserCommand
import com.hermes.application.auth.InviteUserUseCase
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import java.time.Clock
import java.time.Instant

/**
 * Admin wrapper over the existing Auth invitation flow.
 *
 * The actual creation of invited user, pending membership, invitation token,
 * delivery and credential audit remains inside InviteUserUseCase. This wrapper
 * only adapts the result to the Admin General API contract and emits an
 * AdminAccess audit event with an explicit business reason.
 */
class CreateAdminInvitationUseCase(
    private val delegate: InviteUserUseCase,
    private val accessRepository: AdminAccessRepository,
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateAdminInvitationCommand): AdminInvitationCreatedResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_INVITE)

        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Invitation creation reason")

        val created = delegate.execute(
            InviteUserCommand(
                organizationId = organizationId,
                actorUserId = actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                email = command.email,
                displayName = command.displayName,
                roleIds = command.roleIds,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
            )
        )

        val userAccess = accessRepository.findUserAccess(organizationId, created.user.id) ?: AdminUserAccessRecord(
            user = created.user,
            membership = created.membership,
            roles = accessRepository.findRolesByIds(created.membership.roleIds),
            activeSessionCount = 0,
        )

        val roles = accessRepository.findRolesByIds(created.invitation.roleIds)
        val now = Instant.now(clock)

        auditLogger.log(
            AdminAccessAuditEvent(
                action = AdminAccessAuditAction.INVITATION_CREATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = created.invitation.id,
                targetType = "invitation",
                after = created.invitation.toAdminAccessAuditMap(maskToken = true) + mapOf(
                    "userId" to created.user.id,
                    "membershipId" to created.membership.id,
                    "invitationUrlCreated" to (created.invitationUrl != null).toString(),
                ),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminInvitationCreatedResult(
            invitation = created.invitation.toSummary(roles),
            user = userAccess.toDetail(),
            membershipId = created.membership.id,
            rawInvitationToken = created.rawInvitationToken,
            invitationUrl = created.invitationUrl,
            createdAt = created.invitation.createdAt,
        )
    }
}
