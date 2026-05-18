package com.hermes.infrastructure.mongo.catalog

import com.hermes.application.catalog.CatalogAuditAction
import com.hermes.application.catalog.CatalogAuditEvent
import com.hermes.application.catalog.CatalogTemplateSearchQuery
import com.hermes.application.catalog.OrganizationCatalogSearchQuery
import com.hermes.domain.catalog.*
import com.hermes.domain.money.Money
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.core.*
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.*

class MongoCatalogStoreIntegrationTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeEach
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("hermes_catalog_store_test")

        val database = client.getDatabase(databaseName)
        M005CreatePlatformCatalogMigration.up(database)
        M006CreateOrganizationCatalogMigration.up(database)
        M007CreateCatalogRequestsMigration.up(database)
        M017CreateAuditLogsMigration.up(database)
        M021CreateCatalogIdentityFoundationMigration.up(database)
        CatalogMongoBootstrap.ensureIndexes(database)
    }

    @AfterEach
    fun tearDown() {
        if (::client.isInitialized) {
            runCatching { client.getDatabase(databaseName).drop() }
            runCatching { client.close() }
        }
    }

    @Test
    fun `installs mandatory catalog indexes`() {
        val database = client.getDatabase(databaseName)

        assertIndexes(
            database,
            MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES,
            setOf(
                "platform_catalog_templates_global_id_unique_idx",
                "platform_catalog_templates_family_status_idx",
                "platform_catalog_templates_type_status_idx",
                "platform_catalog_templates_normalized_name_idx",
                "platform_catalog_templates_identifier_idx",
                "platform_catalog_templates_business_type_tags_idx",
                "platform_catalog_templates_activity_tags_idx",
            ),
        )
        assertIndexes(
            database,
            MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS,
            setOf(
                "organization_catalog_items_org_status_idx",
                "organization_catalog_items_org_branch_status_idx",
                "organization_catalog_items_org_activity_status_idx",
                "organization_catalog_items_org_searchable_text_ascending_idx",
                "organization_catalog_items_org_identifier_idx",
                "organization_catalog_items_org_global_id_idx",
                "organization_catalog_items_org_template_idx",
            ),
        )
        assertIndexes(
            database,
            MongoCollectionNames.CATALOG_ITEM_REQUESTS,
            setOf(
                "catalog_item_requests_org_status_idx",
                "catalog_item_requests_requested_by_status_idx",
                "catalog_item_requests_org_normalized_name_status_idx",
                "catalog_item_requests_status_created_at_idx",
            ),
        )
        assertIndexes(
            database,
            MongoCollectionNames.CATALOG_PRICE_HISTORY,
            setOf(
                "catalog_price_history_org_item_changed_at_idx",
                "catalog_price_history_org_changed_at_idx",
            ),
        )
    }

    @Test
    fun `stores and searches platform templates by text identifier and type`() {
        val store = MongoCatalogStore(client.getDatabase(databaseName))

        store.templateRepository.create(
            template(
                id = "tpl_cola",
                globalCatalogId = "global_coca_500",
                name = "Coca Cola 500ml",
                ean = "7861001234567"
            )
        )
        store.templateRepository.create(
            template(
                id = "tpl_burger",
                globalCatalogId = "global_burger",
                name = "Hamburguesa Simple",
                ean = "7861007654321"
            )
        )

        val persisted = store.templateRepository.findById("tpl_cola")
        assertNotNull(persisted)
        assertEquals("global_coca_500", persisted.globalCatalogId)
        assertTrue(store.templateRepository.existsByGlobalCatalogId("global_coca_500"))

        val byText = store.templateRepository.search(
            CatalogTemplateSearchQuery(
                query = "cola",
                type = CatalogItemType.PRODUCT,
            ),
        )
        assertEquals(listOf("tpl_cola"), byText.map { it.id })

        val byIdentifier = store.templateRepository.search(
            CatalogTemplateSearchQuery(identifier = "7861001234567"),
        )
        assertEquals("tpl_cola", byIdentifier.single().id)

        assertFailsWith<MongoWriteException> {
            store.templateRepository.create(
                template(
                    id = "tpl_duplicate",
                    globalCatalogId = "global_coca_500",
                    name = "Coca Cola Duplicada",
                    ean = "7861011111111"
                )
            )
        }
    }

    @Test
    fun `stores and searches organization catalog items without leaking organizations`() {
        val store = MongoCatalogStore(client.getDatabase(databaseName))
        store.organizationItemRepository.create(
            localItem(
                id = "ocat_org_1_cola",
                organizationId = "org_1",
                localName = "Coca Cola 500ml",
                sku = "BEB-001"
            )
        )
        store.organizationItemRepository.create(
            localItem(
                id = "ocat_org_2_cola",
                organizationId = "org_2",
                localName = "Coca Cola 500ml",
                sku = "BEB-002"
            )
        )

        val org1Results = store.organizationItemRepository.search(
            OrganizationCatalogSearchQuery(
                organizationId = "org_1",
                query = "cola",
                statuses = setOf(CatalogItemStatus.ACTIVE),
            ),
        )
        assertEquals(listOf("ocat_org_1_cola"), org1Results.map { it.id })

        val org1ByIdentifier = store.organizationItemRepository.search(
            OrganizationCatalogSearchQuery(
                organizationId = "org_1",
                identifier = "BEB001",
            ),
        )
        assertEquals(listOf("ocat_org_1_cola"), org1ByIdentifier.map { it.id })

        val org2ByIdentifier = store.organizationItemRepository.search(
            OrganizationCatalogSearchQuery(
                organizationId = "org_2",
                identifier = "BEB002",
            ),
        )
        assertEquals(listOf("ocat_org_2_cola"), org2ByIdentifier.map { it.id })
    }

    @Test
    fun `detects identifier conflicts scoped by organization and supports exclusion`() {
        val store = MongoCatalogStore(client.getDatabase(databaseName))
        store.organizationItemRepository.create(
            localItem(
                id = "ocat_1",
                organizationId = "org_1",
                localName = "Coca Cola 500ml",
                sku = "BEB-001"
            )
        )
        store.organizationItemRepository.create(
            localItem(
                id = "ocat_2",
                organizationId = "org_2",
                localName = "Coca Cola 500ml",
                sku = "BEB-001"
            )
        )

        assertTrue(store.identifierConflictChecker.existsLocalIdentifier("org_1", "BEB001"))
        assertFalse(store.identifierConflictChecker.existsLocalIdentifier("org_3", "BEB001"))
        assertFalse(
            store.identifierConflictChecker.existsLocalIdentifier(
                "org_1",
                "BEB001",
                excludeCatalogItemId = "ocat_1"
            )
        )
    }

    @Test
    fun `assigns tax profile and preserves previous tax profile id`() {
        val store = MongoCatalogStore(client.getDatabase(databaseName))
        store.organizationItemRepository.create(
            localItem(
                id = "ocat_1",
                organizationId = "org_1",
                localName = "Coca Cola 500ml",
                sku = "BEB-001",
                taxProfileId = "taxp_old"
            )
        )

        val assignment = store.taxProfileRepository.assignTaxProfile(
            organizationId = "org_1",
            catalogItemId = "ocat_1",
            taxProfileId = "taxp_new",
            updatedAt = Instant.parse("2026-05-17T12:00:00Z"),
        )

        assertEquals("taxp_old", assignment.previousTaxProfileId)
        assertEquals("taxp_new", assignment.taxProfileId)
        assertEquals("taxp_new", store.organizationItemRepository.findById("org_1", "ocat_1")!!.taxProfileId)
    }

    @Test
    fun `persists catalog requests and supports pending lookup`() {
        val store = MongoCatalogStore(client.getDatabase(databaseName))
        val request = catalogRequest()

        store.requestRepository.create(request)

        val byId = store.requestRepository.findById(request.id)
        assertNotNull(byId)
        assertEquals("Cuy entero", byId.requestedName)

        val pending = store.requestRepository.findPendingByOrganizationAndName("org_1", "cuy entero")
        assertNotNull(pending)
        assertEquals(request.id, pending.id)

        val reviewed = request.review(
            decision = com.hermes.domain.catalog.CatalogItemRequestDecision.APPROVE,
            reviewerUserId = "usr_admin",
            reason = "Created platform template.",
            reviewedAt = Instant.parse("2026-05-17T13:00:00Z"),
        )
        store.requestRepository.update(reviewed)

        assertNull(store.requestRepository.findPendingByOrganizationAndName("org_1", "cuy entero"))
    }

    @Test
    fun `persists catalog price history and audit logs with Mongo validators enabled`() {
        val database = client.getDatabase(databaseName)
        val store = MongoCatalogStore(database)
        val changedAt = Instant.parse("2026-05-17T12:30:00Z")

        store.priceHistoryRepository.create(
            CatalogPriceHistory(
                id = "cph_1",
                organizationId = "org_1",
                catalogItemId = "ocat_1",
                oldPrice = Money.of("1.00"),
                newPrice = Money.of("1.25"),
                changedByUserId = "usr_owner",
                reason = "Price update test.",
                changedAt = changedAt,
            ),
        )

        val rawPriceHistory = database.getCollection(MongoCollectionNames.CATALOG_PRICE_HISTORY)
            .find(Document("_id", "cph_1"))
            .first()
        assertNotNull(rawPriceHistory)
        assertEquals("org_1", rawPriceHistory.getString("organizationId"))

        MongoCatalogAuditLogger(database).log(
            CatalogAuditEvent(
                action = CatalogAuditAction.LOCAL_ITEM_UPDATED,
                actorUserId = "usr_owner",
                organizationId = "org_1",
                targetId = "ocat_1",
                before = mapOf("price" to "1.00"),
                after = mapOf("price" to "1.25"),
                reason = "Price update test.",
                createdAt = changedAt,
            ),
        )

        val rawAudit = database.getCollection(MongoCollectionNames.AUDIT_LOGS)
            .find(Document("module", "catalog"))
            .first()
        assertNotNull(rawAudit)
        assertEquals("catalog", rawAudit.getString("module"))
        assertEquals("catalog", rawAudit.getString("entityType"))
        assertEquals("ocat_1", rawAudit.getString("entityId"))
    }

    private fun assertIndexes(database: MongoDatabase, collectionName: String, expectedIndexes: Set<String>) {
        val names = database.getCollection(collectionName)
            .listIndexes()
            .into(mutableListOf())
            .mapNotNull { it.getString("name") }
            .toSet()

        expectedIndexes.forEach { expected ->
            assertTrue(expected in names, "Expected index '$expected' in '$collectionName'. Found: $names")
        }
    }

    private fun template(id: String, globalCatalogId: String, name: String, ean: String): PlatformCatalogTemplate =
        PlatformCatalogTemplate(
            id = id,
            globalCatalogId = globalCatalogId,
            canonicalName = name,
            normalizedName = name.lowercase(),
            type = CatalogItemType.PRODUCT,
            status = CatalogTemplateStatus.ACTIVE,
            productFamilyId = "family_soft_drinks",
            variantAttributes = mapOf("presentation" to "500ml"),
            identifiers = listOf(ean13(ean)),
            attributes = mapOf(
                "businessTypeTags" to "restaurant,store",
                "activityTags" to "food_service,retail",
            ),
            media = emptyList<CatalogMediaAsset>(),
        )

    private fun localItem(
        id: String,
        organizationId: String,
        localName: String,
        sku: String,
        taxProfileId: String = "taxp_iva",
    ): OrganizationCatalogItem =
        OrganizationCatalogItem(
            id = id,
            organizationId = organizationId,
            branchId = "br_1",
            activityId = "act_1",
            templateId = "tpl_cola",
            globalCatalogId = "global_coca_500",
            localName = localName,
            searchableText = localName.lowercase(),
            type = CatalogItemType.PRODUCT,
            status = CatalogItemStatus.ACTIVE,
            localPrice = Money.of("1.25"),
            taxProfileId = taxProfileId,
            publicDiscoveryStatus = PublicDiscoveryStatus.PRIVATE,
            productFamilyId = "family_soft_drinks",
            variantAttributes = mapOf("presentation" to "500ml"),
            identifiers = listOf(localSku(sku)),
            attributes = emptyMap(),
            media = emptyList<CatalogMediaAsset>(),
        )

    private fun catalogRequest(): CatalogItemRequest =
        CatalogItemRequest(
            id = "creq_1",
            organizationId = "org_1",
            requestedByUserId = "usr_owner",
            requestedName = "Cuy entero",
            requestedType = CatalogItemType.PRODUCT,
            description = "Producto de restaurante.",
            suggestedCategoryId = "cat_restaurant_main_dishes",
            suggestedTaxProfileCode = "iva_current_full",
            identifiers = emptyList(),
            createdAt = Instant.parse("2026-05-17T12:00:00Z"),
            updatedAt = Instant.parse("2026-05-17T12:00:00Z"),
        )

    private fun ean13(value: String): CatalogIdentifier =
        CatalogIdentifier.create(
            type = CatalogIdentifierType.EAN_13,
            value = value,
            scope = CatalogIdentifierScope.GLOBAL,
            source = CatalogIdentifierSource.PLATFORM,
            status = CatalogIdentifierStatus.VERIFIED,
        )

    private fun localSku(value: String): CatalogIdentifier =
        CatalogIdentifier.create(
            type = CatalogIdentifierType.SKU_LOCAL,
            value = value,
            scope = CatalogIdentifierScope.ORGANIZATION,
            source = CatalogIdentifierSource.ORGANIZATION,
            status = CatalogIdentifierStatus.ACTIVE,
        )
}
