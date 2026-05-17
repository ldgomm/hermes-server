package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemRequestStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CatalogRequestAdvancedUseCasesTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `organization lists only its catalog requests`() {
        val fixture = fixture()
        fixture.requests.create(request("creq_1", "org_1", "Mote"))
        fixture.requests.create(request("creq_2", "org_2", "Café"))

        val result = fixture.listOrganization.execute(
            CatalogListOrganizationRequestsCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
            )
        )

        assertEquals(listOf("creq_1"), result.requests.map { it.id })
    }

    @Test
    fun `admin approves request by creating draft template`() {
        val fixture = fixture()
        fixture.requests.create(request("creq_1", "org_1", "Mote con chicharrón"))

        val result = fixture.approve.execute(
            CatalogApproveRequestAsTemplateCommand(
                requestId = "creq_1",
                actorUserId = "admin_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                globalCatalogId = "food_mote_chicharron",
                canonicalName = "Mote con chicharrón",
                publish = false,
                reason = "Producto válido para restaurantes",
            )
        )

        assertEquals(CatalogItemRequestStatus.APPROVED, result.request.status)
        assertEquals(CatalogTemplateStatus.DRAFT, result.template.status)
        assertEquals(result.template.id, result.request.linkedTemplateId)
        assertNotNull(fixture.templates.findById(result.template.id))
        assertEquals(CatalogAuditAction.CATALOG_ITEM_REQUEST_APPROVED, fixture.audit.events.last().action)
    }

    @Test
    fun `admin approves request and publishes template`() {
        val fixture = fixture()
        fixture.requests.create(request("creq_1", "org_1", "Yahuarlocro"))

        val result = fixture.approve.execute(
            CatalogApproveRequestAsTemplateCommand(
                requestId = "creq_1",
                actorUserId = "admin_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                globalCatalogId = "food_yahuarlocro",
                canonicalName = "Yahuarlocro",
                publish = true,
                reason = "Publicar para piloto restaurante",
            )
        )

        assertEquals(CatalogTemplateStatus.ACTIVE, result.template.status)
    }

    @Test
    fun `rejects request with reason`() {
        val fixture = fixture()
        fixture.requests.create(request("creq_1", "org_1", "Producto duplicado"))

        val result = fixture.reject.execute(
            CatalogRejectRequestCommand(
                requestId = "creq_1",
                actorUserId = "admin_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                reason = "Ya existe en catálogo maestro",
            )
        )

        assertEquals(CatalogItemRequestStatus.REJECTED, result.request.status)
        assertEquals("Ya existe en catálogo maestro", result.request.reviewReason)
    }

    @Test
    fun `links request to existing template`() {
        val fixture = fixture()
        fixture.requests.create(request("creq_1", "org_1", "Café americano"))
        fixture.templates.create(template("tpl_1", "beverage_coffee", "Café", CatalogItemType.PRODUCT))

        val result = fixture.link.execute(
            CatalogLinkRequestToExistingTemplateCommand(
                requestId = "creq_1",
                templateId = "tpl_1",
                actorUserId = "admin_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                reason = "Coincide con plantilla existente",
            )
        )

        assertEquals(CatalogItemRequestStatus.LINKED_TO_EXISTING, result.request.status)
        assertEquals("tpl_1", result.request.linkedTemplateId)
    }

    @Test
    fun `rejects linking request to template with different type`() {
        val fixture = fixture()
        fixture.requests.create(request("creq_1", "org_1", "Servicio técnico", CatalogItemType.SERVICE))
        fixture.templates.create(template("tpl_1", "product_generic", "Producto genérico", CatalogItemType.PRODUCT))

        assertFailsWith<DomainRuleViolation> {
            fixture.link.execute(
                CatalogLinkRequestToExistingTemplateCommand(
                    requestId = "creq_1",
                    templateId = "tpl_1",
                    actorUserId = "admin_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                    reason = "No debería pasar",
                )
            )
        }
    }

    @Test
    fun `requests more information`() {
        val fixture = fixture()
        fixture.requests.create(request("creq_1", "org_1", "Producto ambiguo"))

        val result = fixture.moreInfo.execute(
            CatalogRequestMoreInfoCommand(
                requestId = "creq_1",
                actorUserId = "admin_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                message = "Indica presentación y precio sugerido",
            )
        )

        assertEquals(CatalogItemRequestStatus.NEEDS_MORE_INFO, result.request.status)
        assertEquals("Indica presentación y precio sugerido", result.request.adminMessage)
    }

    private fun fixture(): Fixture {
        val requests = InMemoryCatalogItemRequestRepository()
        val templates = InMemoryPlatformCatalogTemplateRepository()
        val audit = RecordingCatalogAuditLogger()
        val idGenerator = SequenceCatalogIdGenerator()
        return Fixture(
            requests = requests,
            templates = templates,
            audit = audit,
            listOrganization = CatalogListOrganizationRequestsUseCase(requests, audit, clock),
            listAdmin = CatalogListAdminRequestsUseCase(requests, audit, clock),
            approve = CatalogApproveRequestAsTemplateUseCase(requests, templates, idGenerator, audit, clock),
            reject = CatalogRejectRequestUseCase(requests, audit, clock),
            link = CatalogLinkRequestToExistingTemplateUseCase(requests, templates, audit, clock),
            moreInfo = CatalogRequestMoreInfoUseCase(requests, audit, clock),
        )
    }

    private fun request(
        id: String,
        organizationId: String,
        name: String,
        type: CatalogItemType = CatalogItemType.PRODUCT,
    ): CatalogItemRequest = CatalogItemRequest(
        id = id,
        organizationId = organizationId,
        requestedByUserId = "usr_1",
        requestedName = name,
        requestedType = type,
        createdAt = now,
        updatedAt = now,
    )

    private fun template(id: String, globalId: String, name: String, type: CatalogItemType): PlatformCatalogTemplate =
        PlatformCatalogTemplate(
            id = id,
            globalCatalogId = globalId,
            canonicalName = name,
            normalizedName = name.lowercase(),
            type = type,
            status = CatalogTemplateStatus.ACTIVE,
        )

    private data class Fixture(
        val requests: InMemoryCatalogItemRequestRepository,
        val templates: InMemoryPlatformCatalogTemplateRepository,
        val audit: RecordingCatalogAuditLogger,
        val listOrganization: CatalogListOrganizationRequestsUseCase,
        val listAdmin: CatalogListAdminRequestsUseCase,
        val approve: CatalogApproveRequestAsTemplateUseCase,
        val reject: CatalogRejectRequestUseCase,
        val link: CatalogLinkRequestToExistingTemplateUseCase,
        val moreInfo: CatalogRequestMoreInfoUseCase,
    )

    private class InMemoryCatalogItemRequestRepository : CatalogItemRequestRepository, CatalogItemRequestSearchRepository {
        val items = linkedMapOf<String, CatalogItemRequest>()

        override fun create(request: CatalogItemRequest) {
            items[request.id] = request
        }

        override fun update(request: CatalogItemRequest) {
            items[request.id] = request
        }

        override fun findById(requestId: String): CatalogItemRequest? = items[requestId]

        override fun findPendingByOrganizationAndName(organizationId: String, requestedName: String): CatalogItemRequest? =
            items.values.firstOrNull {
                it.organizationId == organizationId &&
                    it.requestedName.equals(requestedName, ignoreCase = true) &&
                    it.status == CatalogItemRequestStatus.PENDING_REVIEW
            }

        override fun search(query: CatalogItemRequestSearchQuery): List<CatalogItemRequest> = items.values
            .filter { query.organizationId == null || it.organizationId == query.organizationId }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }
            .filter { query.requestedType == null || it.requestedType == query.requestedType }
            .filter { query.requestedByUserId == null || it.requestedByUserId == query.requestedByUserId }
            .filter { request ->
                query.query.isNullOrBlank() || request.requestedName.contains(query.query, ignoreCase = true)
            }
            .take(query.limit)
    }

    private class InMemoryPlatformCatalogTemplateRepository : PlatformCatalogTemplateRepository {
        val items = linkedMapOf<String, PlatformCatalogTemplate>()

        override fun create(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        override fun update(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        override fun findById(id: String): PlatformCatalogTemplate? = items[id]
        override fun existsByGlobalCatalogId(globalCatalogId: String): Boolean = items.values.any { it.globalCatalogId == globalCatalogId }
        override fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate> = items.values.toList()
    }

    private class RecordingCatalogAuditLogger : CatalogAuditLogger {
        val events = mutableListOf<CatalogAuditEvent>()
        override fun log(event: CatalogAuditEvent) {
            events += event
        }
    }

    private class SequenceCatalogIdGenerator : CatalogIdGenerator {
        private var next = 1
        override fun newId(prefix: String): String = "${prefix}_${next++}"
    }
}
