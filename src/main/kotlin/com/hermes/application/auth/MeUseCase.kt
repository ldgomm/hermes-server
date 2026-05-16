package com.hermes.application.auth

import com.hermes.domain.organization.MembershipStatus

class MeUseCase(
    private val repository: AuthContextRepository,
    private val authenticateRequestUseCase: AuthenticateRequestUseCase,
    private val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    private val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
) {
    fun execute(command: GetMeCommand): MeResult {
        val principal = authenticateRequestUseCase.execute(command.accessToken)

        val memberships = repository.findMembershipsByUserId(principal.user.id)
            .sortedWith(compareBy({ it.organizationId }, { it.id }))
            .map { membership ->
                val roles = repository.findRolesByIds(membership.roleIds)
                    .sortedBy { it.code }
                MembershipContext(
                    membership = membership,
                    organization = repository.findOrganizationById(membership.organizationId),
                    roles = roles,
                )
            }

        val activeOrganization = activeOrganizationResolverUseCase.execute(
            ResolveActiveOrganizationCommand(
                userId = principal.user.id,
                requestedOrganizationId = command.requestedOrganizationId,
                required = false,
            ),
        )

        val effectivePermissions = activeOrganization?.let { context ->
            effectivePermissionResolverUseCase.execute(
                ResolveEffectivePermissionsCommand(
                    userId = principal.user.id,
                    organizationId = context.organization.id,
                ),
            )
        }

        return MeResult(
            principal = principal,
            memberships = memberships.filter { it.membership.status == MembershipStatus.ACTIVE },
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
        )
    }
}

data class GetMeCommand(
    val accessToken: String,
    val requestedOrganizationId: String? = null,
)
