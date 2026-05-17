package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemRequestStatus
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CatalogListOrganizationRequestsUseCase(
    private val requestSearchRepository: CatalogItemRequestSearchRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogListOrganizationRequestsCommand): CatalogItemRequestsResult {
        assertCanViewOrganizationRequests(command.actorEffectivePermissions)
        val organizationId = command.organizationId.required("Organization id")
        val requests = requestSearchRepository.search(
            CatalogItemRequestSearchQuery(
                organizationId = organizationId,
                statuses = command.statuses,
                requestedType = command.requestedType,
                limit = command.limit.coerceIn(1, 200),
            )
        )
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUEST_LISTED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = null,
                after = mapOf("resultCount" to requests.size.toString()),
                createdAt = Instant.now(clock),
            )
        )
        return CatalogItemRequestsResult(requests)
    }
}

class CatalogListAdminRequestsUseCase(
    private val requestSearchRepository: CatalogItemRequestSearchRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogListAdminRequestsCommand): CatalogItemRequestsResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val requests = requestSearchRepository.search(
            CatalogItemRequestSearchQuery(
                organizationId = command.organizationId.normalizedNullable(),
                statuses = command.statuses,
                requestedType = command.requestedType,
                query = command.query,
                limit = command.limit.coerceIn(1, 300),
            )
        )
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUEST_LISTED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId.normalizedNullable(),
                targetId = null,
                after = mapOf("resultCount" to requests.size.toString(), "scope" to "admin"),
                createdAt = Instant.now(clock),
            )
        )
        return CatalogItemRequestsResult(requests)
    }
}

class CatalogApproveRequestAsTemplateUseCase(
    private val requestRepository: CatalogItemRequestRepository,
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val idGenerator: CatalogIdGenerator,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogApproveRequestAsTemplateCommand): CatalogApproveRequestAsTemplateResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val reason = command.reason.required("Catalog request approval reason")
        val current = requestRepository.findById(command.requestId.required("Catalog request id"))
            ?: throw DomainRuleViolation("Catalog item request does not exist.")
        current.assertOpenForTemplateApproval()

        val canonicalName = command.canonicalName?.required("Catalog template canonical name") ?: current.requestedName.required("Requested item name")
        val globalCatalogId = command.globalCatalogId?.catalogCode("Global catalog id")
            ?: buildGlobalCatalogId(current)
        if (templateRepository.existsByGlobalCatalogId(globalCatalogId)) {
            throw DomainRuleViolation("Platform catalog template global id already exists: $globalCatalogId.")
        }

        val template = PlatformCatalogTemplate(
            id = idGenerator.newId("tpl"),
            globalCatalogId = globalCatalogId,
            canonicalName = canonicalName,
            normalizedName = canonicalName.normalizedSearchText(),
            type = current.requestedType,
            status = if (command.publish) CatalogTemplateStatus.ACTIVE else CatalogTemplateStatus.DRAFT,
            productFamilyId = command.productFamilyId.normalizedNullable(),
            variantAttributes = emptyMap(),
            identifiers = command.identifiers ?: current.identifiers,
            attributes = command.attributes.cleanMap(),
        )
        templateRepository.create(template)

        val updatedRequest = current.approveAsTemplate(
            reviewerUserId = command.actorUserId,
            templateId = template.id,
            reason = reason,
            reviewedAt = now,
        )
        requestRepository.update(updatedRequest)

        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUEST_APPROVED,
                actorUserId = command.actorUserId,
                organizationId = current.organizationId,
                targetId = current.id,
                before = current.toRequestAuditMap(),
                after = updatedRequest.toRequestAuditMap() + mapOf("createdTemplateId" to template.id, "templateStatus" to template.status.name),
                reason = reason,
                createdAt = now,
            )
        )
        return CatalogApproveRequestAsTemplateResult(request = updatedRequest, template = template)
    }
}

class CatalogRejectRequestUseCase(
    private val requestRepository: CatalogItemRequestRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogRejectRequestCommand): CatalogItemRequestResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val reason = command.reason.required("Catalog request rejection reason")
        val current = requestRepository.findById(command.requestId.required("Catalog request id"))
            ?: throw DomainRuleViolation("Catalog item request does not exist.")
        val updated = current.reject(command.actorUserId, reason, now)
        requestRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUEST_REJECTED,
                actorUserId = command.actorUserId,
                organizationId = current.organizationId,
                targetId = current.id,
                before = current.toRequestAuditMap(),
                after = updated.toRequestAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return CatalogItemRequestResult(updated)
    }
}

class CatalogLinkRequestToExistingTemplateUseCase(
    private val requestRepository: CatalogItemRequestRepository,
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogLinkRequestToExistingTemplateCommand): CatalogItemRequestResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val reason = command.reason.required("Catalog request link reason")
        val current = requestRepository.findById(command.requestId.required("Catalog request id"))
            ?: throw DomainRuleViolation("Catalog item request does not exist.")
        val template = templateRepository.findById(command.templateId.required("Catalog template id"))
            ?: throw DomainRuleViolation("Platform catalog template does not exist.")
        if (template.status == CatalogTemplateStatus.ARCHIVED) {
            throw DomainRuleViolation("Cannot link catalog request to archived template.")
        }
        if (template.type != current.requestedType) {
            throw DomainRuleViolation("Catalog request type does not match template type.")
        }
        val updated = current.linkToExistingTemplate(command.actorUserId, template.id, reason, now)
        requestRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUEST_LINKED_TO_EXISTING,
                actorUserId = command.actorUserId,
                organizationId = current.organizationId,
                targetId = current.id,
                before = current.toRequestAuditMap(),
                after = updated.toRequestAuditMap() + mapOf("linkedTemplateGlobalId" to template.globalCatalogId),
                reason = reason,
                createdAt = now,
            )
        )
        return CatalogItemRequestResult(updated)
    }
}

class CatalogRequestMoreInfoUseCase(
    private val requestRepository: CatalogItemRequestRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogRequestMoreInfoCommand): CatalogItemRequestResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val current = requestRepository.findById(command.requestId.required("Catalog request id"))
            ?: throw DomainRuleViolation("Catalog item request does not exist.")
        val updated = current.requestMoreInfo(command.actorUserId, command.message, now)
        requestRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUEST_MORE_INFO_REQUESTED,
                actorUserId = command.actorUserId,
                organizationId = current.organizationId,
                targetId = current.id,
                before = current.toRequestAuditMap(),
                after = updated.toRequestAuditMap(),
                reason = updated.adminMessage,
                createdAt = now,
            )
        )
        return CatalogItemRequestResult(updated)
    }
}

private fun assertCanViewOrganizationRequests(effectivePermissions: Set<String>) {
    val allowed = PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_LOCAL_VIEW) ||
        PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM) ||
        PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
    if (!allowed) {
        throw DomainRuleViolation("Missing any required permission: ${PermissionCatalog.CATALOG_LOCAL_VIEW}, ${PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM}.")
    }
}

private fun CatalogItemRequest.assertOpenForTemplateApproval() {
    if (status != CatalogItemRequestStatus.PENDING_REVIEW && status != CatalogItemRequestStatus.NEEDS_MORE_INFO) {
        throw DomainRuleViolation("Only open catalog item requests can be approved.")
    }
}

private fun buildGlobalCatalogId(request: CatalogItemRequest): String =
    listOf("req", request.organizationId, request.requestedName)
        .joinToString("_")
        .catalogCode("Generated global catalog id")

private fun CatalogItemRequest.toRequestAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "organizationId" to organizationId,
    "requestedName" to requestedName,
    "requestedType" to requestedType.name,
    "status" to status.name,
    "linkedTemplateId" to linkedTemplateId,
    "reviewedByUserId" to reviewedByUserId,
    "reviewReason" to reviewReason,
    "version" to version.toString(),
)

private fun String.required(label: String): String = trim().takeIf { it.isNotBlank() }
    ?: throw DomainRuleViolation("$label cannot be blank.")

private fun String.catalogCode(label: String): String = required(label)
    .lowercase()
    .replace(Regex("[^a-z0-9_\\-]+"), "_")
    .replace(Regex("_+"), "_")
    .trim('_', '-')
    .takeIf { it.isNotBlank() }
    ?: throw DomainRuleViolation("$label is invalid.")

private fun String.normalizedSearchText(): String = trim().lowercase().replace(Regex("\\s+"), " ")
private fun String?.normalizedNullable(): String? = this?.trim()?.takeIf { it.isNotBlank() }
private fun Map<String, String>.cleanMap(): Map<String, String> = entries.mapNotNull { (key, value) ->
    val cleanKey = key.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
    val cleanValue = value.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
    cleanKey to cleanValue
}.toMap()
