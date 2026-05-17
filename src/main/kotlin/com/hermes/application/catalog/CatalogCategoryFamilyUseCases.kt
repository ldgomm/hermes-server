package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogCategoryStatus
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.PlatformCatalogFamily
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CatalogCreateCategoryUseCase(
    private val categoryRepository: CatalogCategoryRepository,
    private val idGenerator: CatalogIdGenerator,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogCreateCategoryCommand): CatalogCategoryResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val code = command.code.catalogCode("Catalog category code")
        if (categoryRepository.existsByCode(code)) {
            throw DomainRuleViolation("Catalog category code already exists: $code.")
        }
        val parentId = command.parentId.normalizedNullable()
        parentId?.let { parent ->
            val parentCategory = categoryRepository.findById(parent)
                ?: throw DomainRuleViolation("Parent catalog category does not exist.")
            parentCategory.assertActive()
        }
        val name = command.name.required("Catalog category name")
        val category = CatalogCategory(
            id = idGenerator.newId("cat"),
            parentId = parentId,
            code = code,
            name = name,
            normalizedName = name.normalizedSearchText(),
            description = command.description.normalizedNullable(),
            businessTypeTags = command.businessTypeTags.cleanTags(),
            activityTags = command.activityTags.cleanTags(),
            status = command.status,
            sortOrder = command.sortOrder,
            createdAt = now,
            updatedAt = now,
        )
        categoryRepository.create(category)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_CATEGORY_CREATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = category.id,
                after = category.toAuditMap(),
                reason = command.reason.normalizedNullable(),
                createdAt = now,
            )
        )
        return CatalogCategoryResult(category)
    }
}

class CatalogUpdateCategoryUseCase(
    private val categoryRepository: CatalogCategoryRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogUpdateCategoryCommand): CatalogCategoryResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val reason = command.reason.required("Catalog category update reason")
        val current = categoryRepository.findById(command.categoryId.required("Catalog category id"))
            ?: throw DomainRuleViolation("Catalog category does not exist.")

        val resolvedParentId = when {
            command.clearParent -> null
            command.parentId != null -> command.parentId.normalizedNullable()
            else -> current.parentId
        }
        if (resolvedParentId == current.id) throw DomainRuleViolation("Catalog category cannot be its own parent.")
        resolvedParentId?.let { parent ->
            val parentCategory = categoryRepository.findById(parent)
                ?: throw DomainRuleViolation("Parent catalog category does not exist.")
            parentCategory.assertActive()
        }

        val name = command.name?.required("Catalog category name") ?: current.name
        val updated = current.copy(
            parentId = resolvedParentId,
            name = name,
            normalizedName = name.normalizedSearchText(),
            description = when {
                command.clearDescription -> null
                command.description != null -> command.description.normalizedNullable()
                else -> current.description
            },
            businessTypeTags = command.businessTypeTags?.cleanTags() ?: current.businessTypeTags,
            activityTags = command.activityTags?.cleanTags() ?: current.activityTags,
            status = command.status ?: current.status,
            sortOrder = command.sortOrder ?: current.sortOrder,
            updatedAt = now,
            version = current.version + 1,
        )
        categoryRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_CATEGORY_UPDATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = updated.id,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return CatalogCategoryResult(updated)
    }
}

class CatalogGetCategoryUseCase(
    private val categoryRepository: CatalogCategoryRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogGetCategoryCommand): CatalogCategoryResult {
        assertCanViewCatalogMaster(command.actorEffectivePermissions)
        val category = categoryRepository.findById(command.categoryId.required("Catalog category id"))
            ?: throw DomainRuleViolation("Catalog category does not exist.")
        auditLogger.log(CatalogAuditEvent(CatalogAuditAction.PLATFORM_CATEGORY_VIEWED, command.actorUserId, null, category.id, createdAt = Instant.now(clock)))
        return CatalogCategoryResult(category)
    }
}

class CatalogSearchCategoriesUseCase(
    private val categoryRepository: CatalogCategoryRepository,
) {
    fun execute(command: CatalogSearchCategoriesCommand): CatalogCategoriesResult {
        assertCanViewCatalogMaster(command.actorEffectivePermissions)
        return CatalogCategoriesResult(
            categoryRepository.search(
                CatalogCategorySearchQuery(
                    parentId = command.parentId.normalizedNullable(),
                    query = command.query,
                    statuses = command.statuses,
                    limit = command.limit.coerceIn(1, 200),
                )
            )
        )
    }
}

class CatalogCreateFamilyUseCase(
    private val familyRepository: PlatformCatalogFamilyRepository,
    private val categoryRepository: CatalogCategoryRepository,
    private val idGenerator: CatalogIdGenerator,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogCreateFamilyCommand): CatalogFamilyResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val globalFamilyId = command.globalFamilyId.catalogCode("Global family id")
        if (familyRepository.existsByGlobalFamilyId(globalFamilyId)) {
            throw DomainRuleViolation("Platform catalog family global id already exists: $globalFamilyId.")
        }
        val categoryId = command.categoryId.normalizedNullable()
        categoryId?.let { categoryRepository.findById(it)?.assertActive() ?: throw DomainRuleViolation("Catalog family category does not exist.") }
        val canonicalName = command.canonicalName.required("Catalog family canonical name")
        val family = PlatformCatalogFamily(
            id = idGenerator.newId("pfam"),
            globalFamilyId = globalFamilyId,
            canonicalName = canonicalName,
            normalizedName = canonicalName.normalizedSearchText(),
            categoryId = categoryId,
            brand = command.brand.normalizedNullable(),
            type = command.type,
            aliases = command.aliases.cleanAliases(),
            attributes = command.attributes.cleanMap(),
            status = command.status,
            createdAt = now,
            updatedAt = now,
        )
        familyRepository.create(family)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_FAMILY_CREATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = family.id,
                after = family.toAuditMap(),
                reason = command.reason.normalizedNullable(),
                createdAt = now,
            )
        )
        return CatalogFamilyResult(family)
    }
}

class CatalogUpdateFamilyUseCase(
    private val familyRepository: PlatformCatalogFamilyRepository,
    private val categoryRepository: CatalogCategoryRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogUpdateFamilyCommand): CatalogFamilyResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val reason = command.reason.required("Catalog family update reason")
        val current = familyRepository.findById(command.familyId.required("Catalog family id"))
            ?: throw DomainRuleViolation("Platform catalog family does not exist.")
        val categoryId = when {
            command.clearCategory -> null
            command.categoryId != null -> command.categoryId.normalizedNullable()
            else -> current.categoryId
        }
        categoryId?.let { categoryRepository.findById(it)?.assertActive() ?: throw DomainRuleViolation("Catalog family category does not exist.") }
        val canonicalName = command.canonicalName?.required("Catalog family canonical name") ?: current.canonicalName
        val updated = current.copy(
            canonicalName = canonicalName,
            normalizedName = canonicalName.normalizedSearchText(),
            categoryId = categoryId,
            brand = when {
                command.clearBrand -> null
                command.brand != null -> command.brand.normalizedNullable()
                else -> current.brand
            },
            type = command.type ?: current.type,
            aliases = command.aliases?.cleanAliases() ?: current.aliases,
            attributes = command.attributes?.cleanMap() ?: current.attributes,
            status = command.status ?: current.status,
            updatedAt = now,
            version = current.version + 1,
        )
        familyRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_FAMILY_UPDATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = updated.id,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return CatalogFamilyResult(updated)
    }
}

class CatalogGetFamilyUseCase(
    private val familyRepository: PlatformCatalogFamilyRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogGetFamilyCommand): CatalogFamilyResult {
        assertCanViewCatalogMaster(command.actorEffectivePermissions)
        val family = familyRepository.findById(command.familyId.required("Catalog family id"))
            ?: throw DomainRuleViolation("Platform catalog family does not exist.")
        auditLogger.log(CatalogAuditEvent(CatalogAuditAction.PLATFORM_FAMILY_VIEWED, command.actorUserId, null, family.id, createdAt = Instant.now(clock)))
        return CatalogFamilyResult(family)
    }
}

class CatalogSearchFamiliesUseCase(
    private val familyRepository: PlatformCatalogFamilyRepository,
) {
    fun execute(command: CatalogSearchFamiliesCommand): CatalogFamiliesResult {
        assertCanViewCatalogMaster(command.actorEffectivePermissions)
        return CatalogFamiliesResult(
            familyRepository.search(
                PlatformCatalogFamilySearchQuery(
                    query = command.query,
                    categoryId = command.categoryId.normalizedNullable(),
                    type = command.type,
                    statuses = command.statuses,
                    limit = command.limit.coerceIn(1, 200),
                )
            )
        )
    }
}

class CatalogGetTemplateUseCase(
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogGetTemplateCommand): CatalogTemplateResult {
        assertCanViewCatalogMaster(command.actorEffectivePermissions)
        val template = templateRepository.findById(command.templateId.required("Catalog template id"))
            ?: throw DomainRuleViolation("Platform catalog template does not exist.")
        auditLogger.log(CatalogAuditEvent(CatalogAuditAction.PLATFORM_TEMPLATE_VIEWED, command.actorUserId, null, template.id, createdAt = Instant.now(clock)))
        return CatalogTemplateResult(template)
    }
}

class CatalogUpdateTemplateUseCase(
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val familyRepository: PlatformCatalogFamilyRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogUpdateTemplateCommand): CatalogTemplateResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val reason = command.reason.required("Catalog template update reason")
        val current = templateRepository.findById(command.templateId.required("Catalog template id"))
            ?: throw DomainRuleViolation("Platform catalog template does not exist.")
        val productFamilyId = when {
            command.clearProductFamily -> null
            command.productFamilyId != null -> command.productFamilyId.normalizedNullable()
            else -> current.productFamilyId
        }
        productFamilyId?.let { familyRepository.findById(it)?.assertActive() ?: throw DomainRuleViolation("Platform catalog family does not exist or is inactive.") }
        val canonicalName = command.canonicalName?.required("Catalog template canonical name") ?: current.canonicalName
        val updated = current.copy(
            canonicalName = canonicalName,
            normalizedName = canonicalName.normalizedSearchText(),
            type = command.type ?: current.type,
            productFamilyId = productFamilyId,
            variantAttributes = command.variantAttributes?.cleanMap() ?: current.variantAttributes,
            identifiers = command.identifiers ?: current.identifiers,
            attributes = command.attributes?.cleanMap() ?: current.attributes,
        )
        templateRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_TEMPLATE_UPDATED,
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = updated.id,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return CatalogTemplateResult(updated)
    }
}

class CatalogChangeTemplateStatusUseCase(
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogChangeTemplateStatusCommand): CatalogTemplateResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)
        val now = Instant.now(clock)
        val reason = command.reason.required("Catalog template status change reason")
        val current = templateRepository.findById(command.templateId.required("Catalog template id"))
            ?: throw DomainRuleViolation("Platform catalog template does not exist.")
        assertValidTemplateStatusTransition(current.status, command.status)
        val updated = current.copy(status = command.status)
        templateRepository.update(updated)
        auditLogger.log(
            CatalogAuditEvent(
                action = command.status.toTemplateAuditAction(),
                actorUserId = command.actorUserId,
                organizationId = null,
                targetId = updated.id,
                before = current.toAuditMap(),
                after = updated.toAuditMap(),
                reason = reason,
                createdAt = now,
            )
        )
        return CatalogTemplateResult(updated)
    }
}

private fun assertCanViewCatalogMaster(effectivePermissions: Set<String>) {
    val allowed = PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER) ||
        PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_LOCAL_VIEW)
    if (!allowed) {
        throw DomainRuleViolation("Missing any required permission: ${PermissionCatalog.CATALOG_MANAGE_MASTER}, ${PermissionCatalog.CATALOG_LOCAL_VIEW}.")
    }
}

private fun assertValidTemplateStatusTransition(current: CatalogTemplateStatus, target: CatalogTemplateStatus) {
    if (current == target) return
    if (current == CatalogTemplateStatus.ARCHIVED && target != CatalogTemplateStatus.ARCHIVED) {
        throw DomainRuleViolation("Archived catalog templates cannot be reactivated.")
    }
    if (target == CatalogTemplateStatus.ACTIVE || target == CatalogTemplateStatus.PAUSED || target == CatalogTemplateStatus.ARCHIVED || target == CatalogTemplateStatus.DRAFT) return
}

private fun CatalogTemplateStatus.toTemplateAuditAction(): CatalogAuditAction = when (this) {
    CatalogTemplateStatus.ACTIVE -> CatalogAuditAction.PLATFORM_TEMPLATE_PUBLISHED
    CatalogTemplateStatus.PAUSED -> CatalogAuditAction.PLATFORM_TEMPLATE_PAUSED
    CatalogTemplateStatus.ARCHIVED -> CatalogAuditAction.PLATFORM_TEMPLATE_ARCHIVED
    CatalogTemplateStatus.DRAFT -> CatalogAuditAction.PLATFORM_TEMPLATE_UPDATED
}

private fun CatalogCategory.toAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "parentId" to parentId,
    "code" to code,
    "name" to name,
    "status" to status.name,
    "sortOrder" to sortOrder.toString(),
    "version" to version.toString(),
)

private fun PlatformCatalogFamily.toAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "globalFamilyId" to globalFamilyId,
    "canonicalName" to canonicalName,
    "categoryId" to categoryId,
    "brand" to brand,
    "type" to type.name,
    "status" to status.name,
    "version" to version.toString(),
)

private fun PlatformCatalogTemplate.toAuditMap(): Map<String, String?> = mapOf(
    "id" to id,
    "globalCatalogId" to globalCatalogId,
    "canonicalName" to canonicalName,
    "type" to type.name,
    "status" to status.name,
    "productFamilyId" to productFamilyId,
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

private fun String?.normalizedNullable(): String? = this?.trim()?.takeIf { it.isNotBlank() }
private fun String.normalizedSearchText(): String = trim().lowercase().replace(Regex("\\s+"), " ")

private fun Set<String>.cleanTags(): Set<String> = mapNotNull { it.trim().lowercase().takeIf(String::isNotBlank) }.toSet()
private fun List<String>.cleanAliases(): List<String> = mapNotNull { it.trim().takeIf(String::isNotBlank) }.distinct()
private fun Map<String, String>.cleanMap(): Map<String, String> = entries.mapNotNull { (key, value) ->
    val k = key.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
    val v = value.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
    k to v
}.toMap()
