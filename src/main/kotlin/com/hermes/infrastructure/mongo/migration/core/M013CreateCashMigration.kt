package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M013CreateCashMigration : MongoMigration {
    override val id: String = "M013_create_cash"
    override val description: String = "Create cash session and cash movement collections."

    override fun up(database: MongoDatabase) {
        createCashSessions(database)
        createCashMovements(database)
    }

    private fun createCashSessions(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("openedBy", MongoMigrationSupport.id(prefix = "usr_"))
            .append("openedAt", MongoMigrationSupport.date())
            .append("status", MongoMigrationSupport.enum(listOf("open", "closing", "closed", "canceled")))
            .append("openingBalance", MongoMigrationSupport.moneyObject())
            .append("expectedCashAmount", MongoMigrationSupport.moneyObject())
            .append("countedCashAmount", MongoMigrationSupport.moneyObject())
            .append("differenceAmount", MongoMigrationSupport.moneyObject())
            .append("closingStartedAt", MongoMigrationSupport.nullableDate())
            .append("closedAt", MongoMigrationSupport.nullableDate())
            .append("canceledAt", MongoMigrationSupport.nullableDate())
            .append("summary", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CASH_SESSIONS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "openedBy",
                    "openedAt",
                    "status",
                    "openingBalance",
                    "expectedCashAmount",
                    "countedCashAmount",
                    "differenceAmount",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "status", "openedAt"),
            "cash_sessions_org_branch_status_opened_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "openedBy", "openedAt"),
            "cash_sessions_org_opened_by_opened_idx"
        )
    }

    private fun createCashMovements(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("cashSessionId", MongoMigrationSupport.id(prefix = "cash_"))
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append(
                "type",
                MongoMigrationSupport.enum(
                    listOf(
                        "opening_balance",
                        "sale_payment",
                        "manual_income",
                        "manual_expense",
                        "withdrawal",
                        "refund",
                        "adjustment"
                    )
                )
            )
            .append("direction", MongoMigrationSupport.enum(listOf("in", "out", "neutral")))
            .append("amount", MongoMigrationSupport.moneyObject())
            .append("occurredAt", MongoMigrationSupport.date())
            .append("referenceType", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("referenceId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("notes", MongoMigrationSupport.nullableString(maxLength = 2048))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CASH_MOVEMENTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "cashSessionId", "branchId", "type", "direction", "amount", "occurredAt",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "cashSessionId", "occurredAt"),
            "cash_movements_org_session_occurred_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "referenceType", "referenceId"),
            "cash_movements_org_reference_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "type", "occurredAt"),
            "cash_movements_org_type_occurred_idx"
        )
    }
}
