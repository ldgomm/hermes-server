package com.hermes.application.admin.access

import com.hermes.application.auth.CreateTemporaryUserCommand
import com.hermes.application.auth.CreateTemporaryUserUseCase
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CreateAdminTemporaryUserUseCase(
    private val delegate: CreateTemporaryUserUseCase,
    private val accessRepository: AdminAccessRepository,
    private val auditLogger: AdminAccessAuditLogger = NoopAdminAccessAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateAdminTemporaryUserCommand): AdminTemporaryUserResult {
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Temporary user creation reason")
        val now = Instant.now(clock)

        val created = delegate.execute(
            CreateTemporaryUserCommand(
                organizationId = organizationId,
                actorUserId = actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                email = command.email,
                displayName = command.displayName,
                roleIds = command.roleIds,
                temporaryPassword = command.temporaryPassword,
                phone = command.phone,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
            )
        )

        val record = accessRepository.findUserAccess(organizationId, created.user.id)
            ?: AdminUserAccessRecord(
                user = created.user,
                membership = created.membership,
                roles = accessRepository.findRolesByIds(created.membership.roleIds),
                activeSessionCount = 0,
            )

        auditLogger.log(
            AdminAccessAuditEvent(
                action = AdminAccessAuditAction.TEMPORARY_USER_CREATED,
                organizationId = organizationId,
                actorUserId = actorUserId,
                targetId = created.user.id,
                targetType = "user_access",
                after = record.toAdminAccessAuditMap() + mapOf(
                    "credentialId" to created.credential.id,
                    "mustChangePassword" to created.credential.mustChangePassword.toString(),
                    "rawTemporaryPasswordReturned" to "true",
                ),
                reason = reason,
                createdAt = now,
            )
        )

        return AdminTemporaryUserResult(
            user = record.toDetail(),
            credentialId = created.credential.id,
            membershipId = created.membership.id,
            temporaryPassword = created.temporaryPassword,
            mustChangePassword = created.credential.mustChangePassword,
            createdAt = now,
        )
    }
}
