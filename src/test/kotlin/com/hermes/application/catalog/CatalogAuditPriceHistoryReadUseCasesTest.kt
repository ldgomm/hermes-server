package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogPriceHistory
import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogAuditPriceHistoryReadUseCasesTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")

    @Test
    fun `lists catalog audit events and writes viewed audit event`() {
        val auditRepository = InMemoryCatalogAuditQueryRepository(
            listOf(
                CatalogAuditRecord(
                    id = "aud_1",
                    action = CatalogAuditAction.LOCAL_ITEM_UPDATED,
                    actorUserId = "usr_1",
                    organizationId = "org_1",
                    targetId = "ocat_1",
                    before = mapOf("price" to "1.00"),
                    after = mapOf("price" to "1.25"),
                    reason = "Price update.",
                    createdAt = now,
                )
            )
        )
        val auditLogger = RecordingCatalogAuditLogger()
        val useCase = CatalogListAuditEventsUseCase(
            auditRepository = auditRepository,
            auditLogger = auditLogger,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        val result = useCase.execute(
            CatalogListAuditEventsCommand(
                organizationId = "org_1",
                actorUserId = "usr_viewer",
                actorEffectivePermissions = setOf(PermissionCatalog.AUDIT_VIEW),
                actions = setOf(CatalogAuditAction.LOCAL_ITEM_UPDATED),
                targetId = "ocat_1",
                limit = 50,
            )
        )

        assertEquals(listOf("aud_1"), result.events.map { it.id })
        assertEquals(CatalogAuditAction.CATALOG_AUDIT_VIEWED, auditLogger.events.single().action)
        assertEquals("1", auditRepository.queries.size.toString())
        assertEquals(setOf(CatalogAuditAction.LOCAL_ITEM_UPDATED), auditRepository.queries.single().actions)
    }

    @Test
    fun `rejects catalog audit read without audit permission`() {
        val useCase = CatalogListAuditEventsUseCase(
            auditRepository = InMemoryCatalogAuditQueryRepository(emptyList()),
            auditLogger = RecordingCatalogAuditLogger(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CatalogListAuditEventsCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                )
            )
        }
    }

    @Test
    fun `lists price history and writes viewed audit event`() {
        val historyRepository = InMemoryCatalogPriceHistoryQueryRepository(
            listOf(
                CatalogPriceHistory(
                    id = "cph_1",
                    organizationId = "org_1",
                    catalogItemId = "ocat_1",
                    oldPrice = Money.of("1.00"),
                    newPrice = Money.of("1.25"),
                    changedByUserId = "usr_1",
                    reason = "Market update.",
                    changedAt = now,
                )
            )
        )
        val auditLogger = RecordingCatalogAuditLogger()
        val useCase = CatalogListPriceHistoryUseCase(
            priceHistoryRepository = historyRepository,
            auditLogger = auditLogger,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        val result = useCase.execute(
            CatalogListPriceHistoryCommand(
                organizationId = "org_1",
                catalogItemId = "ocat_1",
                actorUserId = "usr_viewer",
                actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_PRICE_HISTORY_VIEW),
            )
        )

        assertEquals(listOf("cph_1"), result.history.map { it.id })
        assertEquals(CatalogAuditAction.CATALOG_PRICE_HISTORY_VIEWED, auditLogger.events.single().action)
        assertEquals("ocat_1", historyRepository.queries.single().catalogItemId)
    }

    private class InMemoryCatalogAuditQueryRepository(
        private val records: List<CatalogAuditRecord>,
    ) : CatalogAuditQueryRepository {
        val queries = mutableListOf<CatalogAuditQuery>()

        override fun search(query: CatalogAuditQuery): List<CatalogAuditRecord> {
            queries += query
            return records.filter { record ->
                record.organizationId == query.organizationId &&
                    (query.actions.isEmpty() || record.action in query.actions) &&
                    (query.targetId == null || record.targetId == query.targetId) &&
                    (query.actorUserId == null || record.actorUserId == query.actorUserId)
            }.take(query.limit)
        }
    }

    private class InMemoryCatalogPriceHistoryQueryRepository(
        private val records: List<CatalogPriceHistory>,
    ) : CatalogPriceHistoryQueryRepository {
        val queries = mutableListOf<CatalogPriceHistoryQuery>()

        override fun search(query: CatalogPriceHistoryQuery): List<CatalogPriceHistory> {
            queries += query
            return records.filter { record ->
                record.organizationId == query.organizationId && record.catalogItemId == query.catalogItemId
            }.take(query.limit)
        }
    }

    private class RecordingCatalogAuditLogger : CatalogAuditLogger {
        val events = mutableListOf<CatalogAuditEvent>()
        override fun log(event: CatalogAuditEvent) {
            events += event
        }
    }
}
