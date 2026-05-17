package com.hermes.application.catalog

import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.domain.catalog.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CatalogCreatePlatformTemplateUseCase(
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val idGenerator: CatalogIdGenerator,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogCreatePlatformTemplateCommand): CatalogTemplateResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val globalId = command.globalCatalogId.required("Global catalog id").lowercase()
        val canonicalName = command.canonicalName.required("Canonical name")
        if (templateRepository.existsByGlobalCatalogId(globalId)) {
            throw DomainRuleViolation("Platform catalog template global id already exists: $globalId.")
        }
        val template = PlatformCatalogTemplate(
            id = idGenerator.newId("tpl"),
            globalCatalogId = globalId,
            canonicalName = canonicalName,
            normalizedName = canonicalName.normalizedSearchText(),
            type = command.type,
            status = CatalogTemplateStatus.ACTIVE,
            productFamilyId = command.productFamilyId?.trim()?.takeIf { it.isNotBlank() },
            variantAttributes = command.variantAttributes.cleanMap(),
            identifiers = command.identifiers,
            attributes = command.attributes.cleanMap(),
        )
        templateRepository.create(template)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_TEMPLATE_CREATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = template.id,
                after = mapOf("globalCatalogId" to template.globalCatalogId, "canonicalName" to template.canonicalName),
                reason = command.reason,
                createdAt = now,
            )
        )
        return CatalogTemplateResult(template)
    }
}

class CatalogSearchMasterTemplatesUseCase(
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogSearchMasterTemplatesCommand): CatalogTemplatesResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_LOCAL_VIEW)
        if (command.organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        val result = templateRepository.search(
            CatalogTemplateSearchQuery(
                query = command.query,
                identifier = command.identifier?.normalizeIdentifierSearch(),
                type = command.type,
                onlyActive = true,
                limit = command.limit.coerceIn(1, 100),
            )
        )
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_TEMPLATE_SEARCHED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = null,
                after = mapOf("resultCount" to result.size.toString()),
                createdAt = Instant.now(clock),
            )
        )
        return CatalogTemplatesResult(result)
    }
}

class CatalogCopyTemplateToOrganizationUseCase(
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val itemRepository: OrganizationCatalogItemRepository,
    private val profileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val idGenerator: CatalogIdGenerator,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogCopyTemplateToOrganizationCommand): OrganizationCatalogItemResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER
        )
        val organizationId = command.organizationId.required("Organization id")
        val reason = command.reason.required("Catalog copy reason")
        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")
        val taxProfileCode = command.taxProfileCode.required("Tax profile code").lowercase()
        settings.assertCanUseProfile(taxProfileCode)
        val profile = profileRepository.findByCode(taxProfileCode)
            ?: throw DomainRuleViolation("Tax profile does not exist: $taxProfileCode.")
        val template = templateRepository.findById(command.templateId.required("Template id"))
            ?: throw DomainRuleViolation("Platform catalog template does not exist.")
        if (template.status != CatalogTemplateStatus.ACTIVE) {
            throw DomainRuleViolation("Only active platform catalog templates can be copied to an organization.")
        }
        if (itemRepository.existsByTemplateId(organizationId, template.id)) {
            throw DomainRuleViolation("Catalog template is already copied to this organization.")
        }
        val item = CatalogCopyRules.copyFromTemplate(
            id = idGenerator.newId("ocat"),
            organizationId = organizationId,
            branchId = command.branchId?.trim()?.takeIf { it.isNotBlank() },
            activityId = command.activityId.required("Activity id"),
            template = template,
            localPrice = command.localPrice,
            taxProfileId = profile.id,
        )
        itemRepository.create(item)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.TEMPLATE_COPIED_TO_ORGANIZATION,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = item.id,
                after = mapOf("templateId" to template.id, "taxProfileId" to profile.id),
                reason = reason,
                createdAt = Instant.now(clock),
            )
        )
        return OrganizationCatalogItemResult(item)
    }
}

class CatalogSearchOrganizationItemsUseCase(
    private val itemRepository: OrganizationCatalogItemRepository,
) {
    fun execute(command: CatalogSearchOrganizationItemsCommand): OrganizationCatalogItemsResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_LOCAL_VIEW)
        val organizationId = command.organizationId.required("Organization id")
        return OrganizationCatalogItemsResult(
            itemRepository.search(
                OrganizationCatalogSearchQuery(
                    organizationId = organizationId,
                    query = command.query,
                    identifier = command.identifier?.normalizeIdentifierSearch(),
                    type = command.type,
                    statuses = command.statuses,
                    limit = command.limit.coerceIn(1, 100),
                )
            )
        )
    }
}

class CatalogUpdateLocalItemUseCase(
    private val itemRepository: OrganizationCatalogItemRepository,
    private val profileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val priceHistoryRepository: CatalogPriceHistoryRepository,
    private val identifierConflictChecker: CatalogIdentifierConflictChecker,
    private val idGenerator: CatalogIdGenerator,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogUpdateLocalItemCommand): OrganizationCatalogItemResult {
        val canUpdate = PermissionRules.canPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY
        ) ||
                PermissionRules.canPerform(
                    command.actorEffectivePermissions,
                    PermissionCatalog.CATALOG_LOCAL_CHANGE_PRICE
                ) ||
                PermissionRules.canPerform(
                    command.actorEffectivePermissions,
                    PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE
                )
        if (!canUpdate) throw DomainRuleViolation("Missing catalog local update permission.")
        val organizationId = command.organizationId.required("Organization id")
        val reason = command.reason.required("Catalog update reason")
        val current = itemRepository.findById(organizationId, command.catalogItemId.required("Catalog item id"))
            ?: throw DomainRuleViolation("Organization catalog item does not exist.")
        val now = Instant.now(clock)
        val resolvedTaxProfileId = command.taxProfileCode?.let { code ->
            val profileCode = code.required("Tax profile code").lowercase()
            val settings = settingsRepository.findByOrganizationId(organizationId)
                ?: throw DomainRuleViolation("Organization tax settings do not exist.")
            settings.assertCanUseProfile(profileCode)
            profileRepository.findByCode(profileCode)?.id
                ?: throw DomainRuleViolation("Tax profile does not exist: $profileCode.")
        } ?: current.taxProfileId
        val resolvedIdentifiers = command.identifiers ?: current.identifiers
        assertNoLocalIdentifierConflict(organizationId, current.id, resolvedIdentifiers, identifierConflictChecker)
        val updatedName = command.localName?.required("Local catalog item name") ?: current.localName
        val updated = current.copy(
            localName = updatedName,
            searchableText = searchableText(updatedName, current.globalCatalogId, resolvedIdentifiers),
            localPrice = command.localPrice ?: current.localPrice,
            taxProfileId = resolvedTaxProfileId,
            identifiers = resolvedIdentifiers,
            status = command.status ?: current.status,
        )
        itemRepository.update(updated)
        if (command.localPrice != null && command.localPrice != current.localPrice) {
            priceHistoryRepository.create(
                CatalogPriceHistory(
                    id = idGenerator.newId("cprice"),
                    organizationId = organizationId,
                    catalogItemId = current.id,
                    oldPrice = current.localPrice,
                    newPrice = command.localPrice,
                    changedByUserId = command.actorUserId,
                    reason = reason,
                    changedAt = now,
                )
            )
        }
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.LOCAL_ITEM_UPDATED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = current.id,
                before = mapOf(
                    "localName" to current.localName,
                    "price" to current.localPrice.amount.toPlainString(),
                    "taxProfileId" to current.taxProfileId
                ),
                after = mapOf(
                    "localName" to updated.localName,
                    "price" to updated.localPrice.amount.toPlainString(),
                    "taxProfileId" to updated.taxProfileId
                ),
                reason = reason,
                createdAt = now,
            )
        )
        return OrganizationCatalogItemResult(updated)
    }
}

class CatalogDisableLocalItemUseCase(
    private val itemRepository: OrganizationCatalogItemRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogDisableLocalItemCommand): OrganizationCatalogItemResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY
        )
        val organizationId = command.organizationId.required("Organization id")
        val reason = command.reason.required("Catalog disable reason")
        val current = itemRepository.findById(organizationId, command.catalogItemId.required("Catalog item id"))
            ?: throw DomainRuleViolation("Organization catalog item does not exist.")
        val updated = current.copy(status = CatalogItemStatus.REMOVED_FROM_ACCOUNT)
        itemRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.LOCAL_ITEM_DISABLED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = current.id,
                before = mapOf("status" to current.status.name),
                after = mapOf("status" to updated.status.name),
                reason = reason,
                createdAt = Instant.now(clock),
            )
        )
        return OrganizationCatalogItemResult(updated)
    }
}

class AssignTaxProfileToCatalogItemUseCase(
    private val catalogRepository: OrganizationCatalogTaxProfileRepository,
    private val profileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: AssignTaxProfileToCatalogItemCommand): AssignTaxProfileToCatalogItemResult {
        val allowed = PermissionRules.canPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.TAX_PROFILES_ASSIGN_TO_ITEM
        ) ||
                PermissionRules.canPerform(
                    command.actorEffectivePermissions,
                    PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE
                )
        if (!allowed) throw DomainRuleViolation("Missing tax profile assignment permission.")
        val organizationId = command.organizationId.required("Organization id")
        val reason = command.reason.required("Tax profile assignment reason")
        val profileCode = command.taxProfileCode.required("Tax profile code").lowercase()
        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")
        settings.assertCanUseProfile(profileCode)
        val profile = profileRepository.findByCode(profileCode)
            ?: throw DomainRuleViolation("Tax profile does not exist: $profileCode.")
        val now = Instant.now(clock)
        val assignment = catalogRepository.assignTaxProfile(
            organizationId = organizationId,
            catalogItemId = command.catalogItemId.required("Catalog item id"),
            taxProfileId = profile.id,
            updatedAt = now,
        )
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.LOCAL_ITEM_TAX_PROFILE_ASSIGNED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = assignment.catalogItemId,
                before = mapOf("taxProfileId" to assignment.previousTaxProfileId),
                after = mapOf("taxProfileId" to assignment.taxProfileId, "taxProfileCode" to profile.code),
                reason = reason,
                createdAt = now,
            )
        )
        return AssignTaxProfileToCatalogItemResult(assignment)
    }
}

class CatalogRequestNewItemUseCase(
    private val requestRepository: CatalogItemRequestRepository,
    private val idGenerator: CatalogIdGenerator,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogRequestNewItemCommand): CatalogItemRequestResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM
        )
        val organizationId = command.organizationId.required("Organization id")
        val name = command.requestedName.required("Requested item name")
        requestRepository.findPendingByOrganizationAndName(organizationId, name)?.let {
            throw DomainRuleViolation("A pending catalog request already exists for this item name.")
        }
        val now = Instant.now(clock)
        val request = CatalogItemRequest(
            id = idGenerator.newId("creq"),
            organizationId = organizationId,
            requestedByUserId = command.actorUserId,
            requestedName = name,
            requestedType = command.requestedType,
            description = command.description?.trim()?.takeIf { it.isNotBlank() },
            suggestedCategoryId = command.suggestedCategoryId?.trim()?.takeIf { it.isNotBlank() },
            suggestedTaxProfileCode = command.suggestedTaxProfileCode?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            identifiers = command.identifiers,
            status = CatalogItemRequestStatus.PENDING_REVIEW,
            createdAt = now,
            updatedAt = now,
        )
        requestRepository.create(request)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUESTED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = request.id,
                after = mapOf("requestedName" to request.requestedName, "requestedType" to request.requestedType.name),
                createdAt = now,
            )
        )
        return CatalogItemRequestResult(request)
    }
}

class CatalogReviewRequestUseCase(
    private val requestRepository: CatalogItemRequestRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogReviewRequestCommand): CatalogItemRequestResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val current = requestRepository.findById(command.requestId.required("Catalog request id"))
            ?: throw DomainRuleViolation("Catalog item request does not exist.")
        val reviewed = current.review(
            decision = command.decision,
            reviewerUserId = command.actorUserId,
            reason = command.reason,
            reviewedAt = Instant.now(clock),
        )
        requestRepository.update(reviewed)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_ITEM_REQUEST_REVIEWED,
                actorUserId = command.actorUserId,
                organizationId = reviewed.organizationId,
                targetId = reviewed.id,
                before = mapOf("status" to current.status.name),
                after = mapOf("status" to reviewed.status.name),
                reason = reviewed.reviewReason,
                createdAt = reviewed.updatedAt,
            )
        )
        return CatalogItemRequestResult(reviewed)
    }
}

private fun assertNoLocalIdentifierConflict(
    organizationId: String,
    catalogItemId: String,
    identifiers: List<CatalogIdentifier>,
    checker: CatalogIdentifierConflictChecker,
) {
    identifiers
        .map { it.normalizedValue }
        .filter { it.isNotBlank() }
        .distinct()
        .forEach { normalizedValue ->
            if (checker.existsLocalIdentifier(organizationId, normalizedValue, excludeCatalogItemId = catalogItemId)) {
                throw DomainRuleViolation("Catalog identifier already exists in this organization: $normalizedValue.")
            }
        }
}

private fun searchableText(name: String, globalCatalogId: String, identifiers: List<CatalogIdentifier>): String =
    listOf(name, globalCatalogId, identifiers.searchableIdentifierText()).joinToString(" ").normalizedSearchText()

private fun String.required(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")

private fun String.normalizedSearchText(): String = trim().lowercase().replace(Regex("\\s+"), " ")

private fun String.normalizeIdentifierSearch(): String = trim().filterNot { it == ' ' || it == '-' }.uppercase()

private fun Map<String, String>.cleanMap(): Map<String, String> =
    entries.mapNotNull { (key, value) ->
        val k = key.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val v = value.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        k to v
    }.toMap()
