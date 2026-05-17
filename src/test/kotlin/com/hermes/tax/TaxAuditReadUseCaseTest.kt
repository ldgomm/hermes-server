package com.hermes.application.tax

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaxAuditReadUseCaseTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")

    @Test
    fun `lists tax audit events filtered by organization and action`() {
        val audit = RecordingTaxAuditStore()
        audit.log(
            TaxAuditEvent(
                action = TaxAuditAction.ORGANIZATION_TAX_SETTINGS_UPDATED,
                actorUserId = "usr_1",
                organizationId = "org_1",
                targetId = "taxset_org_1",
                createdAt = now.minusSeconds(10),
            )
        )
        audit.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_RATE_CREATED,
                actorUserId = "usr_1",
                organizationId = null,
                targetId = "taxr_1",
                createdAt = now,
            )
        )

        val useCase = TaxListAuditEventsUseCase(
            auditRepository = audit,
            auditLogger = audit,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        val result = useCase.execute(
            TaxListAuditEventsCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.TAX_SETTINGS_VIEW),
                actions = setOf(TaxAuditAction.ORGANIZATION_TAX_SETTINGS_UPDATED),
            )
        )

        assertEquals(1, result.events.size)
        assertEquals(TaxAuditAction.ORGANIZATION_TAX_SETTINGS_UPDATED, result.events.single().action)
    }

    @Test
    fun `requires audit or tax view permission`() {
        val useCase = TaxListAuditEventsUseCase(RecordingTaxAuditStore())

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                TaxListAuditEventsCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = emptySet(),
                )
            )
        }
    }
}
