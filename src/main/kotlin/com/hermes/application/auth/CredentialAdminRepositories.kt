package com.hermes.application.auth

import com.hermes.domain.credential.PasswordResetToken
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition

interface InvitationRepository {
    fun create(invitation: Invitation)
    fun findInvitationByTokenHash(tokenHash: String): Invitation?
    fun findPendingByOrganizationAndEmail(organizationId: String, email: String): Invitation?
    fun update(invitation: Invitation)
}

interface PasswordResetTokenRepository {
    fun create(token: PasswordResetToken)
    fun findPasswordResetTokenByHash(tokenHash: String): PasswordResetToken?
    fun revokeActiveForUser(userId: String, revokedAt: java.time.Instant): Int
    fun update(token: PasswordResetToken)
}

interface MembershipMutationRepository {
    fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership?
    fun create(membership: OrganizationMembership)
    fun update(membership: OrganizationMembership)
}

interface RoleQueryRepository {
    fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition>
    fun findRoleById(roleId: String): RoleDefinition?
}
