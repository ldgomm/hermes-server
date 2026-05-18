package com.hermes.application.catalog

import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.domain.catalog.*
import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.*
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UseCasesTest {
    private val now: Instant = Instant.parse("2026-05-17T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `creates master template and copies it to organization with valid tax profile`() {
        val fixture = fixture()
        val created = fixture.createTemplate.execute(
            CatalogCreatePlatformTemplateCommand(
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                globalCatalogId = "global_coca_500",
                canonicalName = "Coca Cola 500ml",
                type = CatalogItemType.PRODUCT,
                productFamilyId = "family_soft_drinks",
                variantAttributes = mapOf("presentation" to "500ml"),
                identifiers = listOf(ean("7861001234567")),
            )
        ).template

        val copied = fixture.copyTemplate.execute(
            CatalogCopyTemplateToOrganizationCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER),
                templateId = created.id,
                branchId = "br_1",
                activityId = "act_1",
                localPrice = Money.of("1.25"),
                taxProfileCode = "iva_current_full",
                reason = "Initial catalog setup",
            )
        ).item

        assertEquals("Coca Cola 500ml", copied.localName)
        assertEquals("taxp_iva", copied.taxProfileId)
        assertEquals(CatalogItemStatus.ACTIVE, copied.status)
        assertTrue(fixture.audit.events.any { it.action == CatalogAuditAction.TEMPLATE_COPIED_TO_ORGANIZATION })
    }

    @Test
    fun `rejects copying with disabled organization tax profile`() {
        val fixture = fixture(enabledTaxProfiles = setOf("iva_0"))
        val template = fixture.templateRepository.seedTemplate()

        assertFailsWith<DomainRuleViolation> {
            fixture.copyTemplate.execute(
                CatalogCopyTemplateToOrganizationCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER),
                    templateId = template.id,
                    activityId = "act_1",
                    localPrice = Money.of("1.25"),
                    taxProfileCode = "iva_current_full",
                    reason = "Initial catalog setup",
                )
            )
        }
    }

    @Test
    fun `updates price and records price history`() {
        val fixture = fixture()
        val item = fixture.itemRepository.seedItem()

        val updated = fixture.updateLocalItem.execute(
            CatalogUpdateLocalItemCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_CHANGE_PRICE),
                catalogItemId = item.id,
                localPrice = Money.of("1.50"),
                reason = "Price changed by supplier",
            )
        ).item

        assertEquals("1.50", updated.localPrice.amount.toPlainString())
        assertEquals(1, fixture.priceHistory.items.size)
    }

    @Test
    fun `requests and reviews new catalog item`() {
        val fixture = fixture()
        val request = fixture.requestNewItem.execute(
            CatalogRequestNewItemCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM),
                requestedName = "Empanada de verde",
                requestedType = CatalogItemType.PRODUCT,
                suggestedTaxProfileCode = "iva_current_full",
            )
        ).request

        val reviewed = fixture.reviewRequest.execute(
            CatalogReviewRequestCommand(
                requestId = request.id,
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
                decision = CatalogItemRequestDecision.APPROVE,
                reason = "Valid item",
            )
        ).request

        assertEquals("APPROVED", reviewed.status.name)
    }

    private fun fixture(enabledTaxProfiles: Set<String> = setOf("iva_current_full")): Fixture {
        val templateRepository = InMemoryTemplateRepository()
        val itemRepository = InMemoryItemRepository()
        val requestRepository = InMemoryRequestRepository()
        val priceHistory = InMemoryPriceHistoryRepository()
        val profileRepository = InMemoryTaxProfileRepository()
        val settingsRepository = InMemorySettingsRepository(enabledTaxProfiles)
        val audit = RecordingCatalogAuditLogger()
        val idGenerator = SequenceCatalogIdGenerator()

        return Fixture(
            templateRepository = templateRepository,
            itemRepository = itemRepository,
            priceHistory = priceHistory,
            audit = audit,
            createTemplate = CatalogCreatePlatformTemplateUseCase(templateRepository, idGenerator, audit, clock),
            copyTemplate = CatalogCopyTemplateToOrganizationUseCase(
                templateRepository,
                itemRepository,
                profileRepository,
                settingsRepository,
                idGenerator,
                audit,
                clock
            ),
            updateLocalItem = CatalogUpdateLocalItemUseCase(
                itemRepository,
                profileRepository,
                settingsRepository,
                priceHistory,
                itemRepository,
                idGenerator,
                audit,
                clock
            ),
            requestNewItem = CatalogRequestNewItemUseCase(requestRepository, idGenerator, audit, clock),
            reviewRequest = CatalogReviewRequestUseCase(requestRepository, audit, clock),
        )
    }

    private data class Fixture(
        val templateRepository: InMemoryTemplateRepository,
        val itemRepository: InMemoryItemRepository,
        val priceHistory: InMemoryPriceHistoryRepository,
        val audit: RecordingCatalogAuditLogger,
        val createTemplate: CatalogCreatePlatformTemplateUseCase,
        val copyTemplate: CatalogCopyTemplateToOrganizationUseCase,
        val updateLocalItem: CatalogUpdateLocalItemUseCase,
        val requestNewItem: CatalogRequestNewItemUseCase,
        val reviewRequest: CatalogReviewRequestUseCase,
    )

    private fun ean(value: String): CatalogIdentifier = CatalogIdentifier.create(
        type = CatalogIdentifierType.EAN_13,
        value = value,
        scope = CatalogIdentifierScope.GLOBAL,
        source = CatalogIdentifierSource.PLATFORM,
        status = CatalogIdentifierStatus.VERIFIED,
    )

    private class SequenceCatalogIdGenerator : CatalogIdGenerator {
        private var counter = 0
        override fun newId(prefix: String): String {
            counter += 1
            return "${prefix}_$counter"
        }
    }

    private class RecordingCatalogAuditLogger : CatalogAuditLogger {
        val events = mutableListOf<CatalogAuditEvent>()
        override fun log(event: CatalogAuditEvent) {
            events += event
        }
    }

    private inner class InMemoryTemplateRepository : PlatformCatalogTemplateRepository {
        val items = linkedMapOf<String, PlatformCatalogTemplate>()
        override fun create(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        override fun update(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        override fun findById(id: String): PlatformCatalogTemplate? = items[id]
        override fun existsByGlobalCatalogId(globalCatalogId: String): Boolean =
            items.values.any { it.globalCatalogId == globalCatalogId }

        override fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate> = items.values.toList()
        fun seedTemplate(): PlatformCatalogTemplate = PlatformCatalogTemplate(
            id = "tpl_1",
            globalCatalogId = "global_coca_500",
            canonicalName = "Coca Cola 500ml",
            normalizedName = "coca cola 500ml",
            type = CatalogItemType.PRODUCT,
            status = CatalogTemplateStatus.ACTIVE,
            identifiers = listOf(ean("7861001234567")),
        ).also { create(it) }
    }

    private inner class InMemoryItemRepository : OrganizationCatalogItemRepository, CatalogIdentifierConflictChecker {
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
            items.values.filter { it.organizationId == query.organizationId }

        override fun existsLocalIdentifier(
            organizationId: String,
            normalizedValue: String,
            excludeCatalogItemId: String?
        ): Boolean = items.values.any { item ->
            item.organizationId == organizationId && item.id != excludeCatalogItemId && item.identifiers.any { it.normalizedValue == normalizedValue }
        }

        fun seedItem(): OrganizationCatalogItem = OrganizationCatalogItem(
            id = "ocat_1",
            organizationId = "org_1",
            branchId = "br_1",
            activityId = "act_1",
            templateId = "tpl_1",
            globalCatalogId = "global_coca_500",
            localName = "Coca Cola 500ml",
            searchableText = "coca cola 500ml",
            type = CatalogItemType.PRODUCT,
            status = CatalogItemStatus.ACTIVE,
            localPrice = Money.of("1.25"),
            taxProfileId = "taxp_iva",
            publicDiscoveryStatus = com.hermes.domain.catalog.PublicDiscoveryStatus.PRIVATE,
        ).also { create(it) }
    }

    private class InMemoryRequestRepository : CatalogItemRequestRepository {
        val items = linkedMapOf<String, CatalogItemRequest>()
        override fun create(request: CatalogItemRequest) {
            items[request.id] = request
        }

        override fun update(request: CatalogItemRequest) {
            items[request.id] = request
        }

        override fun findById(requestId: String): CatalogItemRequest? = items[requestId]
        override fun findPendingByOrganizationAndName(
            organizationId: String,
            requestedName: String
        ): CatalogItemRequest? = items.values.firstOrNull {
            it.organizationId == organizationId && it.requestedName.equals(
                requestedName,
                ignoreCase = true
            ) && it.status.name == "PENDING_REVIEW"
        }
    }

    private class InMemoryPriceHistoryRepository : CatalogPriceHistoryRepository {
        val items = mutableListOf<CatalogPriceHistory>()
        override fun create(history: CatalogPriceHistory) {
            items += history
        }
    }

    private inner class InMemoryTaxProfileRepository : TaxProfileRepository {
        override fun create(profile: TaxProfile) = Unit
        override fun update(profile: TaxProfile) = Unit
        override fun findById(id: String): TaxProfile? = profile().takeIf { it.id == id }
        override fun findByCode(code: String): TaxProfile? = profile().takeIf { it.code == code }
        override fun findActive(): List<TaxProfile> = listOf(profile())
        private fun profile(): TaxProfile = TaxProfile(
            id = "taxp_iva",
            code = "iva_current_full",
            name = "IVA vigente",
            treatment = TaxTreatment.IVA_FULL,
            status = TaxProfileStatus.ACTIVE,
            taxRate = TaxRate(
                id = "taxr_iva",
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
            createdAt = now,
            updatedAt = now,
        )
    }

    private inner class InMemorySettingsRepository(private val enabledProfiles: Set<String>) :
        OrganizationTaxSettingsRepository {
        override fun create(settings: OrganizationTaxSettings) = Unit
        override fun update(settings: OrganizationTaxSettings) = Unit
        override fun findByOrganizationId(organizationId: String): OrganizationTaxSettings? = OrganizationTaxSettings(
            id = "taxs_1",
            organizationId = organizationId,
            regime = TaxRegimeCode.GENERAL,
            defaultTaxProfileCode = enabledProfiles.first(),
            enabledTaxProfileCodes = enabledProfiles,
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
}
