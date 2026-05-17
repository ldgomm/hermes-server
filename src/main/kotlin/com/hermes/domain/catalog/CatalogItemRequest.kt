package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

enum class CatalogItemRequestStatus {
    PENDING_REVIEW,
    NEEDS_MORE_INFO,
    APPROVED,
    REJECTED,
    LINKED_TO_EXISTING,
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
    val linkedTemplateId: String? = null,
    val adminMessage: String? = null,
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
        if (status in reviewedStatuses && reviewedByUserId.isNullOrBlank()) {
            throw DomainRuleViolation("Reviewed catalog item request requires reviewer.")
        }
        if (status == CatalogItemRequestStatus.LINKED_TO_EXISTING && linkedTemplateId.isNullOrBlank()) {
            throw DomainRuleViolation("Linked catalog item request requires linked template id.")
        }
        if (status == CatalogItemRequestStatus.APPROVED && linkedTemplateId.isNullOrBlank()) {
            throw DomainRuleViolation("Approved catalog item request requires created template id.")
        }
        if (status == CatalogItemRequestStatus.NEEDS_MORE_INFO && adminMessage.isNullOrBlank()) {
            throw DomainRuleViolation("Catalog item request needing more info requires admin message.")
        }
    }

    fun review(
        decision: CatalogItemRequestDecision,
        reviewerUserId: String,
        reason: String,
        reviewedAt: Instant,
    ): CatalogItemRequest = when (decision) {
        CatalogItemRequestDecision.APPROVE -> approveAsTemplate(
            reviewerUserId = reviewerUserId,
            templateId = "pending-template-link",
            reason = reason,
            reviewedAt = reviewedAt,
        )
        CatalogItemRequestDecision.REJECT -> reject(
            reviewerUserId = reviewerUserId,
            reason = reason,
            reviewedAt = reviewedAt,
        )
    }

    fun approveAsTemplate(
        reviewerUserId: String,
        templateId: String,
        reason: String,
        reviewedAt: Instant,
    ): CatalogItemRequest {
        assertPendingOrNeedsMoreInfo()
        val cleanReviewer = reviewerUserId.required("Catalog request reviewer")
        val cleanTemplateId = templateId.required("Created catalog template id")
        val cleanReason = reason.required("Catalog request approval reason")
        return copy(
            status = CatalogItemRequestStatus.APPROVED,
            reviewedByUserId = cleanReviewer,
            reviewedAt = reviewedAt,
            reviewReason = cleanReason,
            linkedTemplateId = cleanTemplateId,
            adminMessage = null,
            updatedAt = reviewedAt,
            version = version + 1,
        )
    }

    fun reject(
        reviewerUserId: String,
        reason: String,
        reviewedAt: Instant,
    ): CatalogItemRequest {
        assertPendingOrNeedsMoreInfo()
        val cleanReviewer = reviewerUserId.required("Catalog request reviewer")
        val cleanReason = reason.required("Catalog request rejection reason")
        return copy(
            status = CatalogItemRequestStatus.REJECTED,
            reviewedByUserId = cleanReviewer,
            reviewedAt = reviewedAt,
            reviewReason = cleanReason,
            adminMessage = null,
            updatedAt = reviewedAt,
            version = version + 1,
        )
    }

    fun linkToExistingTemplate(
        reviewerUserId: String,
        templateId: String,
        reason: String,
        reviewedAt: Instant,
    ): CatalogItemRequest {
        assertPendingOrNeedsMoreInfo()
        val cleanReviewer = reviewerUserId.required("Catalog request reviewer")
        val cleanTemplateId = templateId.required("Linked catalog template id")
        val cleanReason = reason.required("Catalog request link reason")
        return copy(
            status = CatalogItemRequestStatus.LINKED_TO_EXISTING,
            reviewedByUserId = cleanReviewer,
            reviewedAt = reviewedAt,
            reviewReason = cleanReason,
            linkedTemplateId = cleanTemplateId,
            adminMessage = null,
            updatedAt = reviewedAt,
            version = version + 1,
        )
    }

    fun requestMoreInfo(
        reviewerUserId: String,
        message: String,
        reviewedAt: Instant,
    ): CatalogItemRequest {
        assertPending()
        val cleanReviewer = reviewerUserId.required("Catalog request reviewer")
        val cleanMessage = message.required("Catalog request more info message")
        return copy(
            status = CatalogItemRequestStatus.NEEDS_MORE_INFO,
            reviewedByUserId = cleanReviewer,
            reviewedAt = reviewedAt,
            reviewReason = cleanMessage,
            adminMessage = cleanMessage,
            updatedAt = reviewedAt,
            version = version + 1,
        )
    }

    private fun assertPending() {
        if (status != CatalogItemRequestStatus.PENDING_REVIEW) {
            throw DomainRuleViolation("Only pending catalog item requests can be processed.")
        }
    }

    private fun assertPendingOrNeedsMoreInfo() {
        if (status != CatalogItemRequestStatus.PENDING_REVIEW && status != CatalogItemRequestStatus.NEEDS_MORE_INFO) {
            throw DomainRuleViolation("Only open catalog item requests can be processed.")
        }
    }

    private companion object {
        val reviewedStatuses = setOf(
            CatalogItemRequestStatus.NEEDS_MORE_INFO,
            CatalogItemRequestStatus.APPROVED,
            CatalogItemRequestStatus.REJECTED,
            CatalogItemRequestStatus.LINKED_TO_EXISTING,
        )
    }
}

private fun String.required(label: String): String = trim().takeIf { it.isNotBlank() }
    ?: throw DomainRuleViolation("$label cannot be blank.")
