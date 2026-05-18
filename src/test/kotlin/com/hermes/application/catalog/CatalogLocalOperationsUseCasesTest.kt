package com.hermes.application.catalog

import com.hermes.domain.catalog.*
import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogLocalOperationsUseCasesTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `gets organization catalog item by id`() {
        val fixture = fixture()
        val item = fixture.repository.seedItem(id = "ocat_1")

        val result = fixture.getItem.execute(
            CatalogGetOrganizationItemCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                catalogItemId = item.id,
            )
        )

        assertEquals(item.id, result.item.id)
    }

    @Test
    fun `looks up active organization catalog item by barcode code`() {
        val fixture = fixture()
        fixture.repository.seedItem(id = "ocat_1", identifier = ean("7861001234567"))

        val result = fixture.lookupByCode.execute(
            CatalogLookupOrganizationItemByCodeCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_IDENTIFIERS_SCAN),
                code = "786-1001234567",
            )
        )

        assertEquals("ocat_1", result.item.id)
    }

    @Test
    fun `does not look up inactive item unless explicitly requested`() {
        val fixture = fixture()
        fixture.repository.seedItem(id = "ocat_1", status = CatalogItemStatus.PAUSED, identifier = sku("ALTOS-CUY"))

        assertFailsWith<DomainRuleViolation> {
            fixture.lookupByCode.execute(
                CatalogLookupOrganizationItemByCodeCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                    code = "ALTOS-CUY",
                )
            )
        }

        val found = fixture.lookupByCode.execute(
            CatalogLookupOrganizationItemByCodeCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                code = "ALTOS-CUY",
                includeInactive = true,
            )
        )
        assertEquals("ocat_1", found.item.id)
    }

    @Test
    fun `rejects ambiguous lookup code`() {
        val fixture = fixture()
        fixture.repository.seedItem(id = "ocat_1", identifier = sku("DUP-1"))
        fixture.repository.seedItem(id = "ocat_2", identifier = sku("DUP-1"))

        assertFailsWith<DomainRuleViolation> {
            fixture.lookupByCode.execute(
                CatalogLookupOrganizationItemByCodeCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                    code = "DUP-1",
                )
            )
        }
    }

    @Test
    fun `removes local item from organization account`() {
        val fixture = fixture()
        fixture.repository.seedItem(id = "ocat_1")

        val removed = fixture.removeItem.execute(
            CatalogRemoveLocalItemCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY),
                catalogItemId = "ocat_1",
                reason = "No longer sold",
            )
        ).item

        assertEquals(CatalogItemStatus.REMOVED_FROM_ACCOUNT, removed.status)
        assertTrue(fixture.audit.events.any { it.after["operation"] == "remove_local_copy" })
    }

    private fun fixture(): Fixture {
        val repository = InMemoryOrganizationCatalogItemRepository()
        val audit = RecordingCatalogAuditLogger()
        return Fixture(
            repository = repository,
            audit = audit,
            getItem = CatalogGetOrganizationItemUseCase(repository),
            lookupByCode = CatalogLookupOrganizationItemByCodeUseCase(repository),
            removeItem = CatalogRemoveLocalItemUseCase(repository, audit, clock),
        )
    }

    private data class Fixture(
        val repository: InMemoryOrganizationCatalogItemRepository,
        val audit: RecordingCatalogAuditLogger,
        val getItem: CatalogGetOrganizationItemUseCase,
        val lookupByCode: CatalogLookupOrganizationItemByCodeUseCase,
        val removeItem: CatalogRemoveLocalItemUseCase,
    )

    private class RecordingCatalogAuditLogger : CatalogAuditLogger {
        val events = mutableListOf<CatalogAuditEvent>()
        override fun log(event: CatalogAuditEvent) {
            events += event
        }
    }

    private class InMemoryOrganizationCatalogItemRepository : OrganizationCatalogItemRepository {
        val items = linkedMapOf<String, OrganizationCatalogItem>()

        override fun create(item: OrganizationCatalogItem) {
            items[item.id] = item
        }

        override fun update(item: OrganizationCatalogItem) {
            items[item.id] = item
        }

        override fun findById(organizationId: String, catalogItemId: String): OrganizationCatalogItem? =
            items[catalogItemId]?.takeIf { it.organizationId == organizationId }

        override fun existsByTemplateId(organizationId: String, templateId: String): Boolean =
            items.values.any { it.organizationId == organizationId && it.templateId == templateId }

        override fun search(query: OrganizationCatalogSearchQuery): List<OrganizationCatalogItem> =
            items.values
                .filter { it.organizationId == query.organizationId }
                .filter { query.statuses.isEmpty() || it.status in query.statuses }
                .filter { query.identifier.isNullOrBlank() || it.identifiers.any { identifier -> identifier.normalizedValue == query.identifier } }
                .take(query.limit)

        fun seedItem(
            id: String,
            status: CatalogItemStatus = CatalogItemStatus.ACTIVE,
            identifier: CatalogIdentifier = sku("SKU-$id"),
        ): OrganizationCatalogItem = OrganizationCatalogItem(
            id = id,
            organizationId = "org_1",
            branchId = "br_1",
            activityId = "act_1",
            templateId = "tpl_1",
            globalCatalogId = "global_$id",
            localName = "Item $id",
            searchableText = "item $id",
            type = CatalogItemType.PRODUCT,
            status = status,
            localPrice = Money.of("1.25"),
            taxProfileId = "taxp_iva",
            publicDiscoveryStatus = PublicDiscoveryStatus.PRIVATE,
            identifiers = listOf(identifier),
        ).also { create(it) }
    }

    private companion object {
        fun sku(value: String): CatalogIdentifier = CatalogIdentifier.create(
            type = CatalogIdentifierType.SKU_LOCAL,
            value = value,
            scope = CatalogIdentifierScope.ORGANIZATION,
            source = CatalogIdentifierSource.ORGANIZATION,
            status = CatalogIdentifierStatus.ACTIVE,
        )

        fun ean(value: String): CatalogIdentifier = CatalogIdentifier.create(
            type = CatalogIdentifierType.EAN_13,
            value = value,
            scope = CatalogIdentifierScope.GLOBAL,
            source = CatalogIdentifierSource.PLATFORM,
            status = CatalogIdentifierStatus.VERIFIED,
        )
    }
}
