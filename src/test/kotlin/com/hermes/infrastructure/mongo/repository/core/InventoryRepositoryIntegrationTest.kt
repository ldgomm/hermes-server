package com.hermes.infrastructure.mongo.repository.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Instant

class InventoryRepositoryIntegrationTest {
    @Test
    fun `stock repositories query balances movements and sale references`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_inventory_repository") { database ->
            val balances = StockBalanceRepository(database)
            val movements = StockMovementRepository(database)

            balances.insert(
                RepositoryTestSupport.stockBalance(
                    id = "stock_phase43_available",
                    status = "available",
                    quantityAvailable = "10.000000",
                )
            )
            balances.insert(
                RepositoryTestSupport.stockBalance(
                    id = "stock_phase43_low",
                    status = "low_stock",
                    quantityAvailable = "1.000000",
                ).append("catalogItemId", "item_phase43_low")
            )

            movements.insert(
                RepositoryTestSupport.stockMovement(
                    id = "stmov_phase43_initial",
                    type = "manual_adjustment",
                    direction = "in",
                    quantity = "10.000000",
                    occurredAt = Instant.parse("2026-05-15T09:00:00Z"),
                )
            )
            movements.insert(
                RepositoryTestSupport.stockMovement(
                    id = "stmov_phase43_sale",
                    type = "sale",
                    direction = "out",
                    quantity = "1.000000",
                    occurredAt = Instant.parse("2026-05-15T12:00:00Z"),
                    referenceType = "sale",
                    referenceId = RepositoryTestSupport.SALE_ID,
                )
            )

            val balance = balances.findByItemAndLocation(
                organizationId = RepositoryTestSupport.ORGANIZATION_ID,
                catalogItemId = RepositoryTestSupport.CATALOG_ITEM_ID,
                branchId = RepositoryTestSupport.BRANCH_ID,
            )

            assertNotNull(balance)
            assertEquals("stock_phase43_available", balance?.getString("_id"))

            assertEquals(
                listOf("stock_phase43_low"),
                balances.findLowStock(RepositoryTestSupport.ORGANIZATION_ID)
                    .map { it.getString("_id") }
            )

            assertEquals(
                listOf("stmov_phase43_sale", "stmov_phase43_initial"),
                movements.findByCatalogItem(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.CATALOG_ITEM_ID,
                ).map { it.getString("_id") }
            )

            assertEquals(
                listOf("stmov_phase43_sale"),
                movements.findByReference(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    "sale",
                    RepositoryTestSupport.SALE_ID,
                ).map { it.getString("_id") }
            )
        }
    }
}
