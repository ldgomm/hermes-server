package com.hermes.backend.auth

import com.hermes.application.auth.MeResult
import com.hermes.application.auth.MembershipContext
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.session.UserSession
import kotlinx.serialization.Serializable

@Serializable
data class CurrentSessionResponse(
    val id: String,
    val userId: String,
    val status: String,
    val createdAt: String,
    val expiresAt: String,
    val lastSeenAt: String? = null,
)

@Serializable
data class RoleSummaryResponse(
    val id: String,
    val code: String,
    val scope: String,
    val type: String,
    val name: String,
    val permissionKeys: Set<String>,
    val systemRole: Boolean,
    val critical: Boolean,
)

@Serializable
data class MeMembershipResponse(
    val membership: OrganizationMembershipResponse,
    val organization: OrganizationResponse? = null,
    val roles: List<RoleSummaryResponse>,
)

@Serializable
data class MeResponse(
    val user: UserResponse,
    val currentSession: CurrentSessionResponse,
    val memberships: List<MeMembershipResponse>,
    val activeOrganization: OrganizationResponse? = null,
    val activeMembership: OrganizationMembershipResponse? = null,
    val roles: List<RoleSummaryResponse>,
    val effectivePermissions: Set<String>,
)

fun MeResult.toResponse(): MeResponse = MeResponse(
    user = principal.user.toResponse(),
    currentSession = principal.session.toResponse(),
    memberships = memberships.map { it.toResponse() },
    activeOrganization = activeOrganization?.organization?.toResponse(),
    activeMembership = activeOrganization?.membership?.toResponse(),
    roles = effectivePermissions?.roles?.map { it.toSummaryResponse() }.orEmpty(),
    effectivePermissions = effectivePermissions?.permissions.orEmpty(),
)

fun UserSession.toResponse(): CurrentSessionResponse = CurrentSessionResponse(
    id = id,
    userId = userId,
    status = status.name,
    createdAt = createdAt.toString(),
    expiresAt = expiresAt.toString(),
    lastSeenAt = lastSeenAt?.toString(),
)

fun MembershipContext.toResponse(): MeMembershipResponse = MeMembershipResponse(
    membership = membership.toResponse(),
    organization = organization?.toResponse(),
    roles = roles.map { it.toSummaryResponse() },
)

fun RoleDefinition.toSummaryResponse(): RoleSummaryResponse = RoleSummaryResponse(
    id = id,
    code = code,
    scope = scope.name,
    type = type.name,
    name = name,
    permissionKeys = permissionKeys,
    systemRole = systemRole,
    critical = critical,
)
