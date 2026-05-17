package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

enum class CatalogItemRequestStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    CANCELED,
}

enum class CatalogItemRequestDecision {
    APPROVE,
    REJECT,
}

data class CatalogItemRequest(
    val id: String,
    val organizationId: String,
    val requestedByUserId: String,
    val requestedName: String,
    val requestedType: CatalogItemType,
    val description: String? = null,
    val suggestedCategoryId: String? = null,
    val suggestedTaxProfileCode: String? = null,
    val identifiers: List<CatalogIdentifier> = emptyList(),
    val status: CatalogItemRequestStatus = CatalogItemRequestStatus.PENDING_REVIEW,
    val reviewedByUserId: String? = null,
    val reviewedAt: Instant? = null,
    val reviewReason: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog item request id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id cannot be blank.")
        if (requestedByUserId.isBlank()) throw DomainRuleViolation("Requested by user id cannot be blank.")
        if (requestedName.isBlank()) throw DomainRuleViolation("Requested catalog item name cannot be blank.")
        if (version < 1) throw DomainRuleViolation("Catalog item request version must be positive.")
        if (status == CatalogItemRequestStatus.PENDING_REVIEW && reviewedAt != null) {
            throw DomainRuleViolation("Pending catalog item request cannot have review timestamp.")
        }
        if (status != CatalogItemRequestStatus.PENDING_REVIEW && reviewedByUserId.isNullOrBlank()) {
            throw DomainRuleViolation("Reviewed catalog item request requires reviewer.")
        }
    }

    fun review(
        decision: CatalogItemRequestDecision,
        reviewerUserId: String,
        reason: String,
        reviewedAt: Instant,
    ): CatalogItemRequest {
        if (status != CatalogItemRequestStatus.PENDING_REVIEW) {
            throw DomainRuleViolation("Only pending catalog item requests can be reviewed.")
        }
        if (reviewerUserId.isBlank()) throw DomainRuleViolation("Catalog request reviewer cannot be blank.")
        val cleanReason = reason.trim().takeIf { it.isNotBlank() }
            ?: throw DomainRuleViolation("Catalog request review reason cannot be blank.")

        return copy(
            status = when (decision) {
                CatalogItemRequestDecision.APPROVE -> CatalogItemRequestStatus.APPROVED
                CatalogItemRequestDecision.REJECT -> CatalogItemRequestStatus.REJECTED
            },
            reviewedByUserId = reviewerUserId,
            reviewedAt = reviewedAt,
            reviewReason = cleanReason,
            updatedAt = reviewedAt,
            version = version + 1,
        )
    }
}
