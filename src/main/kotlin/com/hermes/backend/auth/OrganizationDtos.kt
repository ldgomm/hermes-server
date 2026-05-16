package com.hermes.backend.auth

import com.hermes.application.auth.CreateOrganizationResult
import com.hermes.application.auth.CreateOwnerMembershipResult
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrganizationRequest(
    val ownerUserId: String,
    val legalName: String,
    val commercialName: String,
    val taxId: String,
    val countryCode: String = "EC",
)

@Serializable
data class CreateOwnerMembershipRequest(
    val userId: String,
    val organizationId: String,
)

@Serializable
data class OrganizationResponse(
    val id: String,
    val countryCode: String,
    val taxId: String,
    val legalName: String,
    val commercialName: String,
    val status: String,
    val ownerUserId: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class OrganizationMembershipResponse(
    val id: String,
    val organizationId: String,
    val userId: String,
    val roleIds: Set<String>,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val acceptedAt: String? = null,
)

@Serializable
data class CreateOrganizationResponse(
    val organization: OrganizationResponse,
)

@Serializable
data class CreateOwnerMembershipResponse(
    val membership: OrganizationMembershipResponse,
)

fun CreateOrganizationResult.toResponse(): CreateOrganizationResponse = CreateOrganizationResponse(
    organization = organization.toResponse(),
)

fun CreateOwnerMembershipResult.toResponse(): CreateOwnerMembershipResponse = CreateOwnerMembershipResponse(
    membership = membership.toResponse(),
)

fun Organization.toResponse(): OrganizationResponse = OrganizationResponse(
    id = id,
    countryCode = countryCode,
    taxId = taxId,
    legalName = legalName,
    commercialName = commercialName,
    status = status.name,
    ownerUserId = ownerUserId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun OrganizationMembership.toResponse(): OrganizationMembershipResponse = OrganizationMembershipResponse(
    id = id,
    organizationId = organizationId,
    userId = userId,
    roleIds = roleIds,
    status = status.name,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    acceptedAt = acceptedAt?.toString(),
)
