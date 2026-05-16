package com.hermes.infrastructure.mongo.repository.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Instant

class CashRepositoryIntegrationTest {
    @Test
    fun `cash session repository finds only the open session for a branch`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_cash_session_repository") { database ->
            val sessions = CashSessionRepository(database)

            sessions.insert(RepositoryTestSupport.cashSessionDocument())
            sessions.insert(
                RepositoryTestSupport.cashSessionDocument(
                    id = "cash_phase43_closed",
                    status = "closed",
                    openedAt = Instant.parse("2026-05-14T11:00:00Z"),
                )
            )
            sessions.insert(
                RepositoryTestSupport.cashSessionDocument(
                    id = "cash_phase43_other_branch",
                    branchId = RepositoryTestSupport.OTHER_BRANCH_ID,
                    status = "open",
                )
            )

            val open = sessions.findOpenByBranch(
                RepositoryTestSupport.ORGANIZATION_ID,
                RepositoryTestSupport.BRANCH_ID,
            )

            assertNotNull(open)
            assertEquals(RepositoryTestSupport.CASH_SESSION_ID, open?.getString("_id"))

            assertEquals(
                setOf(RepositoryTestSupport.CASH_SESSION_ID, "cash_phase43_closed", "cash_phase43_other_branch"),
                sessions.findByOpenedBy(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.USER_ID,
                ).map { it.getString("_id") }.toSet()
            )
        }
    }

    @Test
    fun `cash movement repository returns ordered movements and reference lookups`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_cash_movement_repository") { database ->
            val movements = CashMovementRepository(database)

            movements.insert(
                RepositoryTestSupport.cashMovementDocument(
                    id = "cmov_phase43_sale",
                    type = "sale_payment",
                    direction = "in",
                    amount = "24.00",
                    occurredAt = Instant.parse("2026-05-15T12:10:00Z"),
                    referenceType = "payment",
                    referenceId = "pay_phase43_001",
                )
            )
            movements.insert(
                RepositoryTestSupport.cashMovementDocument(
                    id = "cmov_phase43_opening",
                    type = "opening_balance",
                    direction = "neutral",
                    amount = "50.00",
                    occurredAt = Instant.parse("2026-05-15T11:00:00Z"),
                )
            )

            assertEquals(
                listOf("cmov_phase43_opening", "cmov_phase43_sale"),
                movements.findByCashSession(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.CASH_SESSION_ID,
                ).map { it.getString("_id") }
            )

            assertEquals(
                listOf("cmov_phase43_sale"),
                movements.findByReference(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    "payment",
                    "pay_phase43_001",
                ).map { it.getString("_id") }
            )
        }
    }
}
