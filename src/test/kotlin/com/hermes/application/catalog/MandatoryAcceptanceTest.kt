package com.hermes.application.catalog

import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogIdentifierScope
import com.hermes.domain.catalog.CatalogIdentifierSource
import com.hermes.domain.catalog.CatalogIdentifierStatus
import com.hermes.domain.catalog.CatalogIdentifierType
import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemRequestStatus
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogPriceHistory
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.catalog.PlatformCatalogFamily
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.catalog.PublicDiscoveryStatus
import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxKind
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxProfileStatus
import com.hermes.domain.tax.TaxRate
import com.hermes.domain.tax.TaxRateStatus
import com.hermes.domain.tax.TaxRegimeCode
import com.hermes.domain.tax.TaxSource
import com.hermes.domain.tax.TaxTreatment
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Fase 7.7 — mandatory acceptance tests for the catalog engine.
 *
 * These tests intentionally exercise the full catalog workflow without Mongo:
 * master governance, local copy, tax-profile assignment bridge, local lookup,
 * price history, request governance and audit read models. The goal is to
 * prevent regressions before sales starts depending on catalog in Fase 8.
 */
class MandatoryAcceptanceTest {
    private val now: Instant = Instant.parse("2026-05-17T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `governed master template can be copied operated audited and prepared for sales`() {
        val fixture = fixture()

        val category = fixture.createCategory.execute(
            CatalogCreateCategoryCommand(
                actorUserId = ADMIN,
                actorEffectivePermissions = masterPermissions,
                code = "Restaurant Main Dishes",
                name = "Platos Fuertes",
                businessTypeTags = setOf("Restaurant"),
                activityTags = setOf("Food_Service"),
                reason = "Categoría base para piloto restaurante.",
            )
        ).category

        val family = fixture.createFamily.execute(
            CatalogCreateFamilyCommand(
                actorUserId = ADMIN,
                actorEffectivePermissions = masterPermissions,
                globalFamilyId = "Cuy Preparado",
                canonicalName = "Cuy preparado",
                categoryId = category.id,
                type = CatalogItemType.PRODUCT,
                aliases = listOf("cuy", "cuy asado"),
                reason = "Familia inicial para platos típicos.",
            )
        ).family

        val template = fixture.createTemplate.execute(
            CatalogCreatePlatformTemplateCommand(
                actorUserId = ADMIN,
                actorEffectivePermissions = masterPermissions,
                globalCatalogId = "food_cuy_entero",
                canonicalName = "Cuy entero",
                type = CatalogItemType.PRODUCT,
                productFamilyId = family.id,
                identifiers = listOf(masterCode("CUY-ENTERO")),
                attributes = mapOf("businessType" to "restaurant"),
                reason = "Plantilla maestra inicial.",
            )
        ).template

        val copied = fixture.copyTemplate.execute(
            CatalogCopyTemplateToOrganizationCommand(
                organizationId = ORG,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER),
                templateId = template.id,
                branchId = "br_main",
                activityId = "act_restaurant",
                localPrice = Money.of("24.00"),
                taxProfileCode = "iva_current_full",
                reason = "Copia inicial para venta.",
            )
        ).item

        assertEquals("Cuy entero", copied.localName)
        assertEquals("taxp_iva_full", copied.taxProfileId)
        assertEquals(CatalogItemStatus.ACTIVE, copied.status)

        val fetched = fixture.getLocalItem.execute(
            CatalogGetOrganizationItemCommand(
                organizationId = ORG,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                catalogItemId = copied.id,
            )
        ).item

        assertEquals(copied.id, fetched.id)

        val lookedUp = fixture.lookupByCode.execute(
            CatalogLookupOrganizationItemByCodeCommand(
                organizationId = ORG,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_IDENTIFIERS_SCAN),
                code = "CUY ENTERO",
            )
        ).item

        assertEquals(copied.id, lookedUp.id)

        val repriced = fixture.updateLocalItem.execute(
            CatalogUpdateLocalItemCommand(
                organizationId = ORG,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_CHANGE_PRICE),
                catalogItemId = copied.id,
                localPrice = Money.of("25.00"),
                reason = "Ajuste de precio por costo.",
            )
        ).item

        assertEquals("25.00", repriced.localPrice.amount.toPlainString())

        val priceHistory = fixture.listPriceHistory.execute(
            CatalogListPriceHistoryCommand(
                organizationId = ORG,
                catalogItemId = copied.id,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_PRICE_HISTORY_VIEW),
            )
        )

        assertEquals(1, priceHistory.history.size)
        assertEquals("24.00", priceHistory.history.single().oldPrice.amount.toPlainString())
        assertEquals("25.00", priceHistory.history.single().newPrice.amount.toPlainString())

        val assignment = fixture.assignTaxProfile.execute(
            AssignTaxProfileToCatalogItemCommand(
                organizationId = ORG,
                catalogItemId = copied.id,
                taxProfileCode = "iva_0",
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE),
                reason = "Caso de prueba de puente tributario.",
            )
        ).assignment

        assertEquals("taxp_iva_full", assignment.previousTaxProfileId)
        assertEquals("taxp_iva_zero", assignment.taxProfileId)

        val removed = fixture.removeLocalItem.execute(
            CatalogRemoveLocalItemCommand(
                organizationId = ORG,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY),
                catalogItemId = copied.id,
                reason = "Retirar temporalmente del negocio.",
            )
        ).item

        assertEquals(CatalogItemStatus.REMOVED_FROM_ACCOUNT, removed.status)

        assertFailsWith<DomainRuleViolation> {
            fixture.lookupByCode.execute(
                CatalogLookupOrganizationItemByCodeCommand(
                    organizationId = ORG,
                    actorUserId = OWNER,
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_IDENTIFIERS_SCAN),
                    code = "CUY ENTERO",
                )
            )
        }

        val audit = fixture.listAudit.execute(
            CatalogListAuditEventsCommand(
                organizationId = ORG,
                actorUserId = ADMIN,
                actorEffectivePermissions = setOf(PermissionCatalog.AUDIT_VIEW),
                actions = setOf(
                    CatalogAuditAction.TEMPLATE_COPIED_TO_ORGANIZATION,
                    CatalogAuditAction.LOCAL_ITEM_UPDATED,
                    CatalogAuditAction.LOCAL_ITEM_TAX_PROFILE_ASSIGNED,
                    CatalogAuditAction.LOCAL_ITEM_DISABLED,
                ),
                limit = 100,
            )
        )

        assertEquals(
            setOf(
                CatalogAuditAction.TEMPLATE_COPIED_TO_ORGANIZATION,
                CatalogAuditAction.LOCAL_ITEM_UPDATED,
                CatalogAuditAction.LOCAL_ITEM_TAX_PROFILE_ASSIGNED,
                CatalogAuditAction.LOCAL_ITEM_DISABLED,
            ),
            audit.events.map { it.action }.toSet(),
        )
        assertTrue(fixture.audit.events.any { it.action == CatalogAuditAction.CATALOG_AUDIT_VIEWED })
        assertNotNull(fixture.items.findById(ORG, copied.id))
    }

    @Test
    fun `mandatory safeguards reject unsafe catalog operations`() {
        val fixture = fixture()

        val pausedTemplate = fixture.templates.createAndReturn(
            template(
                id = "tpl_paused",
                globalCatalogId = "food_paused",
                canonicalName = "Producto pausado",
                status = CatalogTemplateStatus.PAUSED,
                identifiers = listOf(masterCode("PAUSED-001")),
            )
        )

        assertFailsWith<DomainRuleViolation> {
            fixture.copyTemplate.execute(
                CatalogCopyTemplateToOrganizationCommand(
                    organizationId = ORG,
                    actorUserId = OWNER,
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER),
                    templateId = pausedTemplate.id,
                    activityId = "act_restaurant",
                    localPrice = Money.of("1.00"),
                    taxProfileCode = "iva_current_full",
                    reason = "No debe copiar plantillas pausadas.",
                )
            )
        }

        val first = fixture.items.createAndReturn(
            localItem(
                id = "ocat_first",
                globalCatalogId = "global_first",
                localName = "Primer producto",
                identifiers = listOf(localCode("DUP-001")),
            )
        )
        val second = fixture.items.createAndReturn(
            localItem(
                id = "ocat_second",
                globalCatalogId = "global_second",
                localName = "Segundo producto",
                identifiers = listOf(localCode("DUP-002")),
            )
        )

        assertFailsWith<DomainRuleViolation> {
            fixture.updateLocalItem.execute(
                CatalogUpdateLocalItemCommand(
                    organizationId = ORG,
                    actorUserId = OWNER,
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY),
                    catalogItemId = second.id,
                    identifiers = listOf(localCode("DUP-001")),
                    reason = "No debe duplicar identificador local.",
                )
            )
        }

        fixture.items.create(
            first.copy(id = "ocat_conflict", globalCatalogId = "global_conflict")
        )

        assertFailsWith<DomainRuleViolation> {
            fixture.lookupByCode.execute(
                CatalogLookupOrganizationItemByCodeCommand(
                    organizationId = ORG,
                    actorUserId = OWNER,
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_IDENTIFIERS_SCAN),
                    code = "DUP-001",
                )
            )
        }

        val request = fixture.requests.createAndReturn(
            request(id = "creq_service_mismatch", requestedType = CatalogItemType.PRODUCT)
        )
        fixture.templates.create(
            template(
                id = "tpl_service",
                globalCatalogId = "service_delivery",
                canonicalName = "Delivery",
                type = CatalogItemType.SERVICE,
                identifiers = emptyList(),
            )
        )

        assertFailsWith<DomainRuleViolation> {
            fixture.linkRequest.execute(
                CatalogLinkRequestToExistingTemplateCommand(
                    requestId = request.id,
                    templateId = "tpl_service",
                    actorUserId = ADMIN,
                    actorEffectivePermissions = masterPermissions,
                    reason = "No debe vincular tipos diferentes.",
                )
            )
        }
    }

    @Test
    fun `advanced request lifecycle lists asks more information approves and publishes template`() {
        val fixture = fixture()

        val request = fixture.requestNewItem.execute(
            CatalogRequestNewItemCommand(
                organizationId = ORG,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM),
                requestedName = "Yahuarlocro",
                requestedType = CatalogItemType.PRODUCT,
                description = "Sopa tradicional para restaurante.",
                suggestedTaxProfileCode = "iva_current_full",
                identifiers = listOf(localCode("YAH-001")),
            )
        ).request

        val organizationList = fixture.listOrganizationRequests.execute(
            CatalogListOrganizationRequestsCommand(
                organizationId = ORG,
                actorUserId = OWNER,
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                statuses = setOf(CatalogItemRequestStatus.PENDING_REVIEW),
            )
        )

        assertEquals(listOf(request.id), organizationList.requests.map { it.id })

        val needsInfo = fixture.requestMoreInfo.execute(
            CatalogRequestMoreInfoCommand(
                requestId = request.id,
                actorUserId = ADMIN,
                actorEffectivePermissions = masterPermissions,
                message = "Confirmar si lleva IVA o tarifa 0.",
            )
        ).request

        assertEquals(CatalogItemRequestStatus.NEEDS_MORE_INFO, needsInfo.status)

        val approved = fixture.approveRequest.execute(
            CatalogApproveRequestAsTemplateCommand(
                requestId = request.id,
                actorUserId = ADMIN,
                actorEffectivePermissions = masterPermissions,
                globalCatalogId = "food_yahuarlocro",
                canonicalName = "Yahuarlocro",
                publish = true,
                reason = "Producto válido para piloto restaurante.",
            )
        )

        assertEquals(CatalogItemRequestStatus.APPROVED, approved.request.status)
        assertEquals(CatalogTemplateStatus.ACTIVE, approved.template.status)
        assertEquals(approved.template.id, approved.request.linkedTemplateId)

        val adminList = fixture.listAdminRequests.execute(
            CatalogListAdminRequestsCommand(
                actorUserId = ADMIN,
                actorEffectivePermissions = masterPermissions,
                organizationId = ORG,
                statuses = setOf(CatalogItemRequestStatus.APPROVED),
            )
        )

        assertEquals(listOf(request.id), adminList.requests.map { it.id })
        assertTrue(fixture.audit.events.any { it.action == CatalogAuditAction.CATALOG_ITEM_REQUEST_MORE_INFO_REQUESTED })
        assertTrue(fixture.audit.events.any { it.action == CatalogAuditAction.CATALOG_ITEM_REQUEST_APPROVED })
    }

    private fun fixture(): Fixture {
        val categories = InMemoryCategoryRepository()
        val families = InMemoryFamilyRepository()
        val templates = InMemoryTemplateRepository()
        val items = InMemoryItemRepository()
        val requests = InMemoryRequestRepository()
        val priceHistory = InMemoryPriceHistoryRepository()
        val profiles = InMemoryTaxProfileRepository()
        val settings = InMemorySettingsRepository()
        val audit = RecordingCatalogAuditStore()
        val ids = SequenceCatalogIdGenerator()

        return Fixture(
            categories = categories,
            families = families,
            templates = templates,
            items = items,
            requests = requests,
            priceHistory = priceHistory,
            audit = audit,
            createCategory = CatalogCreateCategoryUseCase(categories, ids, audit, clock),
            createFamily = CatalogCreateFamilyUseCase(families, categories, ids, audit, clock),
            createTemplate = CatalogCreatePlatformTemplateUseCase(templates, ids, audit, clock),
            copyTemplate = CatalogCopyTemplateToOrganizationUseCase(templates, items, profiles, settings, ids, audit, clock),
            getLocalItem = CatalogGetOrganizationItemUseCase(items),
            lookupByCode = CatalogLookupOrganizationItemByCodeUseCase(items),
            updateLocalItem = CatalogUpdateLocalItemUseCase(items, profiles, settings, priceHistory, items, ids, audit, clock),
            assignTaxProfile = AssignTaxProfileToCatalogItemUseCase(items, profiles, settings, audit, clock),
            removeLocalItem = CatalogRemoveLocalItemUseCase(items, audit, clock),
            requestNewItem = CatalogRequestNewItemUseCase(requests, ids, audit, clock),
            listOrganizationRequests = CatalogListOrganizationRequestsUseCase(requests, audit, clock),
            listAdminRequests = CatalogListAdminRequestsUseCase(requests, audit, clock),
            approveRequest = CatalogApproveRequestAsTemplateUseCase(requests, templates, ids, audit, clock),
            linkRequest = CatalogLinkRequestToExistingTemplateUseCase(requests, templates, audit, clock),
            requestMoreInfo = CatalogRequestMoreInfoUseCase(requests, audit, clock),
            listAudit = CatalogListAuditEventsUseCase(audit, audit, clock),
            listPriceHistory = CatalogListPriceHistoryUseCase(priceHistory, audit, clock),
        )
    }

    private data class Fixture(
        val categories: InMemoryCategoryRepository,
        val families: InMemoryFamilyRepository,
        val templates: InMemoryTemplateRepository,
        val items: InMemoryItemRepository,
        val requests: InMemoryRequestRepository,
        val priceHistory: InMemoryPriceHistoryRepository,
        val audit: RecordingCatalogAuditStore,
        val createCategory: CatalogCreateCategoryUseCase,
        val createFamily: CatalogCreateFamilyUseCase,
        val createTemplate: CatalogCreatePlatformTemplateUseCase,
        val copyTemplate: CatalogCopyTemplateToOrganizationUseCase,
        val getLocalItem: CatalogGetOrganizationItemUseCase,
        val lookupByCode: CatalogLookupOrganizationItemByCodeUseCase,
        val updateLocalItem: CatalogUpdateLocalItemUseCase,
        val assignTaxProfile: AssignTaxProfileToCatalogItemUseCase,
        val removeLocalItem: CatalogRemoveLocalItemUseCase,
        val requestNewItem: CatalogRequestNewItemUseCase,
        val listOrganizationRequests: CatalogListOrganizationRequestsUseCase,
        val listAdminRequests: CatalogListAdminRequestsUseCase,
        val approveRequest: CatalogApproveRequestAsTemplateUseCase,
        val linkRequest: CatalogLinkRequestToExistingTemplateUseCase,
        val requestMoreInfo: CatalogRequestMoreInfoUseCase,
        val listAudit: CatalogListAuditEventsUseCase,
        val listPriceHistory: CatalogListPriceHistoryUseCase,
    )

    private inner class SequenceCatalogIdGenerator : CatalogIdGenerator {
        private var counter: Int = 0
        override fun newId(prefix: String): String {
            counter += 1
            return "${prefix}_$counter"
        }
    }

    private class InMemoryCategoryRepository : CatalogCategoryRepository {
        private val items = linkedMapOf<String, CatalogCategory>()

        override fun create(category: CatalogCategory) {
            items[category.id] = category
        }

        override fun update(category: CatalogCategory) {
            items[category.id] = category
        }

        override fun findById(id: String): CatalogCategory? = items[id]

        override fun findByCode(code: String): CatalogCategory? =
            items.values.firstOrNull { it.code == code.trim().lowercase() }

        override fun existsByCode(code: String): Boolean = findByCode(code) != null

        override fun search(query: CatalogCategorySearchQuery): List<CatalogCategory> =
            items.values
                .asSequence()
                .filter { query.parentId == null || it.parentId == query.parentId }
                .filter { query.statuses.isEmpty() || it.status in query.statuses }
                .filter { category ->
                    query.query.isNullOrBlank() ||
                        category.normalizedName.contains(query.query.trim().lowercase()) ||
                        category.code.contains(query.query.trim().lowercase())
                }
                .take(query.limit)
                .toList()
    }

    private class InMemoryFamilyRepository : PlatformCatalogFamilyRepository {
        private val items = linkedMapOf<String, PlatformCatalogFamily>()

        override fun create(family: PlatformCatalogFamily) {
            items[family.id] = family
        }

        override fun update(family: PlatformCatalogFamily) {
            items[family.id] = family
        }

        override fun findById(id: String): PlatformCatalogFamily? = items[id]

        override fun findByGlobalFamilyId(globalFamilyId: String): PlatformCatalogFamily? =
            items.values.firstOrNull { it.globalFamilyId == globalFamilyId.trim().lowercase() }

        override fun existsByGlobalFamilyId(globalFamilyId: String): Boolean =
            findByGlobalFamilyId(globalFamilyId) != null

        override fun search(query: PlatformCatalogFamilySearchQuery): List<PlatformCatalogFamily> =
            items.values
                .asSequence()
                .filter { query.categoryId == null || it.categoryId == query.categoryId }
                .filter { query.type == null || it.type == query.type }
                .filter { query.statuses.isEmpty() || it.status in query.statuses }
                .filter { family ->
                    query.query.isNullOrBlank() ||
                        family.normalizedName.contains(query.query.trim().lowercase()) ||
                        family.globalFamilyId.contains(query.query.trim().lowercase())
                }
                .take(query.limit)
                .toList()
    }

    private class InMemoryTemplateRepository : PlatformCatalogTemplateRepository {
        private val items = linkedMapOf<String, PlatformCatalogTemplate>()

        override fun create(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        fun createAndReturn(template: PlatformCatalogTemplate): PlatformCatalogTemplate =
            template.also(::create)

        override fun update(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        override fun findById(id: String): PlatformCatalogTemplate? = items[id]

        override fun existsByGlobalCatalogId(globalCatalogId: String): Boolean =
            items.values.any { it.globalCatalogId == globalCatalogId.trim().lowercase() }

        override fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate> =
            items.values
                .asSequence()
                .filter { !query.onlyActive || it.status == CatalogTemplateStatus.ACTIVE }
                .filter { query.type == null || it.type == query.type }
                .filter { template ->
                    query.identifier == null ||
                        template.identifiers.any { it.normalizedValue == query.identifier }
                }
                .filter { template ->
                    query.query.isNullOrBlank() ||
                        template.normalizedName.contains(query.query.trim().lowercase()) ||
                        template.globalCatalogId.contains(query.query.trim().lowercase())
                }
                .take(query.limit)
                .toList()
    }

    private class InMemoryItemRepository :
        OrganizationCatalogItemRepository,
        OrganizationCatalogTaxProfileRepository,
        CatalogIdentifierConflictChecker {
        private val items = linkedMapOf<String, OrganizationCatalogItem>()

        override fun create(item: OrganizationCatalogItem) {
            items[item.id] = item
        }

        fun createAndReturn(item: OrganizationCatalogItem): OrganizationCatalogItem =
            item.also(::create)

        override fun update(item: OrganizationCatalogItem) {
            items[item.id] = item
        }

        override fun findById(organizationId: String, catalogItemId: String): OrganizationCatalogItem? =
            items[catalogItemId]?.takeIf { it.organizationId == organizationId }

        override fun existsByTemplateId(organizationId: String, templateId: String): Boolean =
            items.values.any { it.organizationId == organizationId && it.templateId == templateId }

        override fun search(query: OrganizationCatalogSearchQuery): List<OrganizationCatalogItem> =
            items.values
                .asSequence()
                .filter { it.organizationId == query.organizationId }
                .filter { query.statuses.isEmpty() || it.status in query.statuses }
                .filter { query.type == null || it.type == query.type }
                .filter { item ->
                    query.identifier == null ||
                        item.identifiers.any { it.normalizedValue == query.identifier }
                }
                .filter { item ->
                    query.query.isNullOrBlank() ||
                        item.searchableText.contains(query.query.trim().lowercase()) ||
                        item.localName.lowercase().contains(query.query.trim().lowercase())
                }
                .take(query.limit)
                .toList()

        override fun existsLocalIdentifier(
            organizationId: String,
            normalizedValue: String,
            excludeCatalogItemId: String?,
        ): Boolean =
            items.values.any { item ->
                item.organizationId == organizationId &&
                    item.id != excludeCatalogItemId &&
                    item.identifiers.any { it.normalizedValue == normalizedValue }
            }

        override fun assignTaxProfile(
            organizationId: String,
            catalogItemId: String,
            taxProfileId: String,
            updatedAt: Instant,
        ): CatalogTaxProfileAssignmentRecord {
            val current = findById(organizationId, catalogItemId)
                ?: throw DomainRuleViolation("Organization catalog item does not exist.")
            val updated = current.copy(taxProfileId = taxProfileId)
            update(updated)
            return CatalogTaxProfileAssignmentRecord(
                organizationId = organizationId,
                catalogItemId = catalogItemId,
                previousTaxProfileId = current.taxProfileId,
                taxProfileId = taxProfileId,
                updatedAt = updatedAt,
            )
        }
    }

    private class InMemoryRequestRepository : CatalogItemRequestRepository, CatalogItemRequestSearchRepository {
        private val items = linkedMapOf<String, CatalogItemRequest>()

        override fun create(request: CatalogItemRequest) {
            items[request.id] = request
        }

        fun createAndReturn(request: CatalogItemRequest): CatalogItemRequest =
            request.also(::create)

        override fun update(request: CatalogItemRequest) {
            items[request.id] = request
        }

        override fun findById(requestId: String): CatalogItemRequest? = items[requestId]

        override fun findPendingByOrganizationAndName(
            organizationId: String,
            requestedName: String,
        ): CatalogItemRequest? =
            items.values.firstOrNull {
                it.organizationId == organizationId &&
                    it.requestedName.equals(requestedName.trim(), ignoreCase = true) &&
                    it.status == CatalogItemRequestStatus.PENDING_REVIEW
            }

        override fun search(query: CatalogItemRequestSearchQuery): List<CatalogItemRequest> =
            items.values
                .asSequence()
                .filter { query.organizationId == null || it.organizationId == query.organizationId }
                .filter { query.statuses.isEmpty() || it.status in query.statuses }
                .filter { query.requestedType == null || it.requestedType == query.requestedType }
                .filter { query.requestedByUserId == null || it.requestedByUserId == query.requestedByUserId }
                .filter { request ->
                    query.query.isNullOrBlank() ||
                        request.requestedName.lowercase().contains(query.query.trim().lowercase())
                }
                .take(query.limit)
                .toList()
    }

    private class InMemoryPriceHistoryRepository :
        CatalogPriceHistoryRepository,
        CatalogPriceHistoryQueryRepository {
        private val items = mutableListOf<CatalogPriceHistory>()

        override fun create(history: CatalogPriceHistory) {
            items += history
        }

        override fun search(query: CatalogPriceHistoryQuery): List<CatalogPriceHistory> =
            items
                .asSequence()
                .filter { it.organizationId == query.organizationId }
                .filter { it.catalogItemId == query.catalogItemId }
                .filter { query.from == null || !it.changedAt.isBefore(query.from) }
                .filter { query.to == null || !it.changedAt.isAfter(query.to) }
                .sortedByDescending { it.changedAt }
                .take(query.limit)
                .toList()
    }

    private class RecordingCatalogAuditStore : CatalogAuditLogger, CatalogAuditQueryRepository {
        val events = mutableListOf<CatalogAuditEvent>()

        override fun log(event: CatalogAuditEvent) {
            events += event
        }

        override fun search(query: CatalogAuditQuery): List<CatalogAuditRecord> =
            events
                .asSequence()
                .withIndex()
                .filter { (_, event) -> event.organizationId == query.organizationId }
                .filter { (_, event) -> query.actions.isEmpty() || event.action in query.actions }
                .filter { (_, event) -> query.actorUserId == null || event.actorUserId == query.actorUserId }
                .filter { (_, event) -> query.targetId == null || event.targetId == query.targetId }
                .filter { (_, event) -> query.from == null || !event.createdAt.isBefore(query.from) }
                .filter { (_, event) -> query.to == null || !event.createdAt.isAfter(query.to) }
                .map { (index, event) ->
                    CatalogAuditRecord(
                        id = "caud_${index + 1}",
                        action = event.action,
                        actorUserId = event.actorUserId,
                        organizationId = event.organizationId ?: query.organizationId,
                        targetId = event.targetId,
                        before = event.before,
                        after = event.after,
                        reason = event.reason,
                        createdAt = event.createdAt,
                    )
                }
                .take(query.limit)
                .toList()
    }

    private inner class InMemoryTaxProfileRepository : TaxProfileRepository {
        private val profiles = listOf(ivaProfile(), zeroProfile()).associateBy { it.code }

        override fun create(profile: TaxProfile) = Unit
        override fun update(profile: TaxProfile) = Unit
        override fun findById(id: String): TaxProfile? = profiles.values.firstOrNull { it.id == id }
        override fun findByCode(code: String): TaxProfile? = profiles[code.trim().lowercase()]
        override fun findActive(): List<TaxProfile> = profiles.values.toList()

        private fun ivaProfile(): TaxProfile = TaxProfile(
            id = "taxp_iva_full",
            code = "iva_current_full",
            name = "IVA vigente",
            treatment = TaxTreatment.IVA_FULL,
            status = TaxProfileStatus.ACTIVE,
            taxRate = TaxRate(
                id = "taxr_iva_full",
                code = "iva_15",
                name = "IVA 15%",
                kind = TaxKind.IVA,
                rate = BigDecimal("15.0000"),
                status = TaxRateStatus.ACTIVE,
                sriTaxCode = "2",
                sriRateCode = "4",
                legalBasis = "Test",
                effectiveFrom = now.minusSeconds(60),
                source = TaxSource.SYSTEM_SEED,
                createdAt = now,
                updatedAt = now,
            ),
            sriTaxCode = "2",
            sriRateCode = "4",
            legalBasis = "Test",
            effectiveFrom = now.minusSeconds(60),
            source = TaxSource.SYSTEM_SEED,
            createdAt = now,
            updatedAt = now,
        )

        private fun zeroProfile(): TaxProfile = TaxProfile(
            id = "taxp_iva_zero",
            code = "iva_0",
            name = "IVA 0%",
            treatment = TaxTreatment.IVA_ZERO,
            status = TaxProfileStatus.ACTIVE,
            taxRate = TaxRate(
                id = "taxr_iva_zero",
                code = "iva_0",
                name = "IVA 0%",
                kind = TaxKind.IVA,
                rate = BigDecimal("0.0000"),
                status = TaxRateStatus.ACTIVE,
                sriTaxCode = "2",
                sriRateCode = "0",
                legalBasis = "Test",
                effectiveFrom = now.minusSeconds(60),
                source = TaxSource.SYSTEM_SEED,
                createdAt = now,
                updatedAt = now,
            ),
            sriTaxCode = "2",
            sriRateCode = "0",
            legalBasis = "Test",
            effectiveFrom = now.minusSeconds(60),
            source = TaxSource.SYSTEM_SEED,
            createdAt = now,
            updatedAt = now,
        )
    }

    private inner class InMemorySettingsRepository : OrganizationTaxSettingsRepository {
        override fun create(settings: OrganizationTaxSettings) = Unit
        override fun update(settings: OrganizationTaxSettings) = Unit

        override fun findByOrganizationId(organizationId: String): OrganizationTaxSettings =
            OrganizationTaxSettings(
                id = "taxset_$organizationId",
                organizationId = organizationId,
                regime = TaxRegimeCode.RIMPE_ENTREPRENEUR,
                defaultTaxProfileCode = "iva_current_full",
                enabledTaxProfileCodes = setOf("iva_current_full", "iva_0"),
                allowTaxInclusivePrices = true,
                allowManualLineDiscounts = true,
                requireTaxProfileForCatalogItems = true,
                status = OrganizationTaxSettingsStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                createdBy = "system",
                updatedBy = "system",
            )
    }

    private fun template(
        id: String,
        globalCatalogId: String,
        canonicalName: String,
        type: CatalogItemType = CatalogItemType.PRODUCT,
        status: CatalogTemplateStatus = CatalogTemplateStatus.ACTIVE,
        identifiers: List<CatalogIdentifier> = listOf(masterCode(globalCatalogId)),
    ): PlatformCatalogTemplate =
        PlatformCatalogTemplate(
            id = id,
            globalCatalogId = globalCatalogId,
            canonicalName = canonicalName,
            normalizedName = canonicalName.trim().lowercase(),
            type = type,
            status = status,
            identifiers = identifiers,
        )

    private fun localItem(
        id: String,
        globalCatalogId: String,
        localName: String,
        identifiers: List<CatalogIdentifier>,
        status: CatalogItemStatus = CatalogItemStatus.ACTIVE,
    ): OrganizationCatalogItem =
        OrganizationCatalogItem(
            id = id,
            organizationId = ORG,
            branchId = "br_main",
            activityId = "act_restaurant",
            templateId = "tpl_$id",
            globalCatalogId = globalCatalogId,
            localName = localName,
            searchableText = localName.trim().lowercase(),
            type = CatalogItemType.PRODUCT,
            status = status,
            localPrice = Money.of("1.00"),
            taxProfileId = "taxp_iva_full",
            publicDiscoveryStatus = PublicDiscoveryStatus.PRIVATE,
            identifiers = identifiers,
        )

    private fun request(
        id: String,
        requestedType: CatalogItemType,
    ): CatalogItemRequest =
        CatalogItemRequest(
            id = id,
            organizationId = ORG,
            requestedByUserId = OWNER,
            requestedName = "Solicitud $id",
            requestedType = requestedType,
            status = CatalogItemRequestStatus.PENDING_REVIEW,
            createdAt = now,
            updatedAt = now,
        )

    private fun masterCode(value: String): CatalogIdentifier =
        CatalogIdentifier.create(
            type = CatalogIdentifierType.INTERNAL_CODE,
            value = value,
            scope = CatalogIdentifierScope.GLOBAL,
            source = CatalogIdentifierSource.PLATFORM,
            status = CatalogIdentifierStatus.VERIFIED,
            isPrimary = true,
        )

    private fun localCode(value: String): CatalogIdentifier =
        CatalogIdentifier.create(
            type = CatalogIdentifierType.INTERNAL_CODE,
            value = value,
            scope = CatalogIdentifierScope.ORGANIZATION,
            source = CatalogIdentifierSource.ORGANIZATION,
            status = CatalogIdentifierStatus.ACTIVE,
            isPrimary = true,
        )

    private companion object {
        const val ORG = "org_1"
        const val OWNER = "usr_owner"
        const val ADMIN = "usr_admin"

        val masterPermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER)
    }
}
