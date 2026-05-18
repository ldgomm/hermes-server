package com.hermes.infrastructure.mongo.catalog

import com.hermes.application.catalog.CatalogAuditAction
import com.hermes.application.catalog.CatalogAuditEvent
import com.hermes.application.catalog.CatalogAuditQuery
import com.hermes.application.catalog.CatalogPriceHistoryQuery
import com.hermes.domain.catalog.CatalogPriceHistory
import com.hermes.domain.money.Money
import com.hermes.infrastructure.mongo.migration.core.M006CreateOrganizationCatalogMigration
import com.hermes.infrastructure.mongo.migration.core.M017CreateAuditLogsMigration
import com.hermes.infrastructure.mongo.migration.core.M021CreateCatalogIdentityFoundationMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class MongoCatalogReadStoreIntegrationTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeEach
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("hermes_catalog_read_store_test")

        val database = client.getDatabase(databaseName)
        M006CreateOrganizationCatalogMigration.up(database)
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
    fun `searches catalog audit logs by organization action and target`() {
        val database = client.getDatabase(databaseName)
        val logger = MongoCatalogAuditLogger(database)
        val readStore = MongoCatalogReadStore(database)
        val occurredAt = Instant.parse("2026-05-17T12:00:00Z")

        logger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.LOCAL_ITEM_UPDATED,
                actorUserId = "usr_1",
                organizationId = "org_1",
                targetId = "ocat_1",
                before = mapOf("price" to "1.00"),
                after = mapOf("price" to "1.25"),
                reason = "Price update.",
                createdAt = occurredAt,
            )
        )
        logger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.LOCAL_ITEM_UPDATED,
                actorUserId = "usr_2",
                organizationId = "org_2",
                targetId = "ocat_2",
                createdAt = occurredAt,
            )
        )

        val result = readStore.auditQueryRepository.search(
            CatalogAuditQuery(
                organizationId = "org_1",
                actions = setOf(CatalogAuditAction.LOCAL_ITEM_UPDATED),
                targetId = "ocat_1",
            )
        )

        assertEquals(1, result.size)
        assertEquals("org_1", result.single().organizationId)
        assertEquals("ocat_1", result.single().targetId)
        assertEquals("1.25", result.single().after.getValue("price"))
    }

    @Test
    fun `searches catalog price history by organization item and date range`() {
        val database = client.getDatabase(databaseName)
        val store = MongoCatalogStore(database)
        val readStore = MongoCatalogReadStore(database)

        store.priceHistoryRepository.create(
            CatalogPriceHistory(
                id = "cph_1",
                organizationId = "org_1",
                catalogItemId = "ocat_1",
                oldPrice = Money.of("1.00"),
                newPrice = Money.of("1.25"),
                changedByUserId = "usr_1",
                reason = "First update.",
                changedAt = Instant.parse("2026-05-17T12:00:00Z"),
            )
        )
        store.priceHistoryRepository.create(
            CatalogPriceHistory(
                id = "cph_2",
                organizationId = "org_1",
                catalogItemId = "ocat_1",
                oldPrice = Money.of("1.25"),
                newPrice = Money.of("1.50"),
                changedByUserId = "usr_1",
                reason = "Second update.",
                changedAt = Instant.parse("2026-05-18T12:00:00Z"),
            )
        )
        store.priceHistoryRepository.create(
            CatalogPriceHistory(
                id = "cph_other_org",
                organizationId = "org_2",
                catalogItemId = "ocat_1",
                oldPrice = Money.of("1.00"),
                newPrice = Money.of("2.00"),
                changedByUserId = "usr_2",
                reason = "Other org.",
                changedAt = Instant.parse("2026-05-18T12:00:00Z"),
            )
        )

        val result = readStore.priceHistoryQueryRepository.search(
            CatalogPriceHistoryQuery(
                organizationId = "org_1",
                catalogItemId = "ocat_1",
                from = Instant.parse("2026-05-18T00:00:00Z"),
            )
        )

        assertEquals(listOf("cph_2"), result.map { it.id })
        assertEquals(Money.of("1.50"), result.single().newPrice)
    }
}
