package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.repository.MongoDuplicateEntityException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class SaleRepositoryIntegrationTest {
    @Test
    fun `sale repository queries sales by organization number customer status and date range`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_sale_repository") { database ->
            val sales = SaleRepository(database)

            sales.insert(
                RepositoryTestSupport.saleDocument(
                    id = "sale_phase43_open_1",
                    saleNumber = "S-000001",
                    operationalStatus = "pending",
                    createdAt = Instant.parse("2026-05-15T10:00:00Z"),
                )
            )
            sales.insert(
                RepositoryTestSupport.saleDocument(
                    id = "sale_phase43_open_2",
                    saleNumber = "S-000002",
                    operationalStatus = "confirmed",
                    createdAt = Instant.parse("2026-05-15T11:00:00Z"),
                )
            )
            sales.insert(
                RepositoryTestSupport.saleDocument(
                    id = "sale_phase43_closed",
                    saleNumber = "S-000003",
                    operationalStatus = "closed",
                    paymentStatus = "paid",
                    collectionStatus = "settled",
                    documentStatus = "authorized",
                    createdAt = Instant.parse("2026-05-15T12:00:00Z"),
                )
            )
            sales.insert(
                RepositoryTestSupport.saleDocument(
                    id = "sale_phase43_other_org",
                    organizationId = RepositoryTestSupport.OTHER_ORGANIZATION_ID,
                    saleNumber = "S-000001",
                    operationalStatus = "pending",
                    createdAt = Instant.parse("2026-05-15T13:00:00Z"),
                )
            )

            val byNumber = sales.findBySaleNumber(RepositoryTestSupport.ORGANIZATION_ID, " S-000001 ")
            assertNotNull(byNumber)
            assertEquals("sale_phase43_open_1", byNumber?.getString("_id"))

            val openIds = sales.findOpenByOrganization(RepositoryTestSupport.ORGANIZATION_ID)
                .map { it.getString("_id") }
                .toSet()

            assertEquals(setOf("sale_phase43_open_1", "sale_phase43_open_2"), openIds)
            assertEquals(
                listOf("sale_phase43_closed", "sale_phase43_open_2", "sale_phase43_open_1"),
                sales.findByCustomer(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.CUSTOMER_ID,
                ).map { it.getString("_id") }
            )

            val inRange = sales.findByActivityAndDateRange(
                organizationId = RepositoryTestSupport.ORGANIZATION_ID,
                activityId = RepositoryTestSupport.ACTIVITY_ID,
                from = java.util.Date.from(Instant.parse("2026-05-15T10:30:00Z")),
                to = java.util.Date.from(Instant.parse("2026-05-15T12:30:00Z")),
            ).map { it.getString("_id") }

            assertEquals(listOf("sale_phase43_closed", "sale_phase43_open_2"), inRange)
        }
    }

    @Test
    fun `sale repository preserves organization scoped unique sale numbers`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_sale_uniqueness") { database ->
            val sales = SaleRepository(database)

            sales.insert(
                RepositoryTestSupport.saleDocument(
                    id = "sale_phase43_unique_1",
                    saleNumber = "S-UNIQUE",
                )
            )

            sales.insert(
                RepositoryTestSupport.saleDocument(
                    id = "sale_phase43_unique_other_org",
                    organizationId = RepositoryTestSupport.OTHER_ORGANIZATION_ID,
                    saleNumber = "S-UNIQUE",
                )
            )

            val duplicate = assertThrows(MongoDuplicateEntityException::class.java) {
                sales.insert(
                    RepositoryTestSupport.saleDocument(
                        id = "sale_phase43_unique_2",
                        saleNumber = "S-UNIQUE",
                    )
                )
            }

            assertTrue(duplicate.message.orEmpty().contains("already exists"))
        }
    }
}
