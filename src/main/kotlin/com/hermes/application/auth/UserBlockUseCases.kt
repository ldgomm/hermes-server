package com.hermes.application.auth

import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class BlockUserUseCase(
    private val userRepository: UserRepository,
    private val membershipRepository: MembershipMutationRepository,
    private val sessionRepository: UserSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: BlockUserCommand): UserBlockResult {
        val now = Instant.now(clock)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_BLOCK)
        if (command.actorUserId == command.targetUserId) {
            throw DomainRuleViolation("User cannot block themselves.")
        }

        val user = userRepository.findUserById(command.targetUserId)
            ?: throw DomainRuleViolation("Target user does not exist.")
        val membership = membershipRepository.findByOrganizationIdAndUserId(command.organizationId, user.id)
            ?: throw DomainRuleViolation("Target user does not belong to this organization.")

        val blockedUser = user.block(command.reason, now)
        userRepository.update(blockedUser)

        val suspendedMembership = membership.copy(
            status = MembershipStatus.SUSPENDED,
            updatedAt = now,
            version = membership.version + 1,
        )
        membershipRepository.update(suspendedMembership)

        val sessions = sessionRepository.findActiveByUserId(user.id)
        sessions.forEach { session -> sessionRepository.update(session.revoke(now, command.reason)) }
        val revokedTokens = refreshTokenRepository.revokeActiveBySessionIds(sessions.map { it.id }.toSet(), now)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.USER_BLOCKED,
                actorUserId = command.actorUserId,
                targetUserId = user.id,
                organizationId = command.organizationId,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                message = command.reason,
                createdAt = now,
            )
        )

        return UserBlockResult(
            user = blockedUser,
            membership = suspendedMembership,
            revokedSessions = sessions.size,
            revokedRefreshTokens = revokedTokens,
        )
    }
}

class UnblockUserUseCase(
    private val userRepository: UserRepository,
    private val membershipRepository: MembershipMutationRepository,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UnblockUserCommand): UserUnblockResult {
        val now = Instant.now(clock)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_UNBLOCK)

        val user = userRepository.findUserById(command.targetUserId)
            ?: throw DomainRuleViolation("Target user does not exist.")
        val membership = membershipRepository.findByOrganizationIdAndUserId(command.organizationId, user.id)
            ?: throw DomainRuleViolation("Target user does not belong to this organization.")

        val unblockedUser = user.unblock(now)
        userRepository.update(unblockedUser)

        val activeMembership = if (membership.status == MembershipStatus.SUSPENDED) {
            membership.copy(
                status = MembershipStatus.ACTIVE,
                updatedAt = now,
                version = membership.version + 1,
            ).also(membershipRepository::update)
        } else {
            membership
        }

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.USER_UNBLOCKED,
                actorUserId = command.actorUserId,
                targetUserId = user.id,
                organizationId = command.organizationId,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                message = command.reason,
                createdAt = now,
            )
        )

        return UserUnblockResult(
            user = unblockedUser,
            membership = activeMembership,
        )
    }
}
