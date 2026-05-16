package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M016CreateInventoryMigration : MongoMigration {
    override val id: String = "M016_create_inventory"
    override val description: String = "Create stock balances, stock movements and stock reservations."

    override fun up(database: MongoDatabase) {
        createStockBalances(database)
        createStockMovements(database)
        createStockReservations(database)
    }

    private fun createStockBalances(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("catalogItemId", MongoMigrationSupport.id(prefix = "item_"))
            .append("stockUnit", MongoMigrationSupport.string(maxLength = 32))
            .append("quantityOnHand", MongoMigrationSupport.decimal())
            .append("quantityReserved", MongoMigrationSupport.decimal())
            .append("quantityAvailable", MongoMigrationSupport.decimal())
            .append("lowStockThreshold", MongoMigrationSupport.decimal())
            .append("status", MongoMigrationSupport.enum(listOf("available", "low_stock", "out_of_stock", "disabled")))
            .append("lastMovementAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.STOCK_BALANCES,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "catalogItemId",
                    "stockUnit",
                    "quantityOnHand",
                    "quantityReserved",
                    "quantityAvailable",
                    "lowStockThreshold",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "catalogItemId"),
            "stock_balances_org_branch_item_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "updatedAt"),
            "stock_balances_org_status_updated_idx"
        )
    }

    private fun createStockMovements(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("catalogItemId", MongoMigrationSupport.id(prefix = "item_"))
            .append(
                "type",
                MongoMigrationSupport.enum(
                    listOf(
                        "sale",
                        "reservation",
                        "release",
                        "manual_adjustment",
                        "return",
                        "warranty_replacement",
                        "import_commit",
                        "correction"
                    )
                )
            )
            .append("direction", MongoMigrationSupport.enum(listOf("in", "out", "neutral")))
            .append("quantity", MongoMigrationSupport.decimal())
            .append("unitCode", MongoMigrationSupport.string(maxLength = 32))
            .append("occurredAt", MongoMigrationSupport.date())
            .append("referenceType", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("referenceId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("reason", MongoMigrationSupport.nullableString(maxLength = 2048))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.STOCK_MOVEMENTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "catalogItemId",
                    "type",
                    "direction",
                    "quantity",
                    "unitCode",
                    "occurredAt"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "catalogItemId", "occurredAt"),
            "stock_movements_org_branch_item_occurred_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "referenceType", "referenceId"),
            "stock_movements_org_reference_idx",
            sparse = true
        )
    }

    private fun createStockReservations(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("catalogItemId", MongoMigrationSupport.id(prefix = "item_"))
            .append("saleId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("reservationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("quantity", MongoMigrationSupport.decimal())
            .append("unitCode", MongoMigrationSupport.string(maxLength = 32))
            .append(
                "status",
                MongoMigrationSupport.enum(listOf("active", "consumed", "released", "expired", "canceled"))
            )
            .append("expiresAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.STOCK_RESERVATIONS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "catalogItemId",
                    "quantity",
                    "unitCode",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "catalogItemId", "status"),
            "stock_reservations_org_branch_item_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "saleId", "status"),
            "stock_reservations_org_sale_status_idx",
            sparse = true
        )
    }
}
