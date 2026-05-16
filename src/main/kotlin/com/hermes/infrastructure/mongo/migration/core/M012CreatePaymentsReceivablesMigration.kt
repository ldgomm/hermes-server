package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M012CreatePaymentsReceivablesMigration : MongoMigration {
    override val id: String = "M012_create_payments_receivables"
    override val description: String =
        "Create payments and receivables collections for cash, transfer, card and credit flows."

    override fun up(database: MongoDatabase) {
        createPayments(database)
        createReceivables(database)
    }

    private fun createPayments(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("saleId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("customerId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("cashSessionId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append(
                "method",
                MongoMigrationSupport.enum(
                    listOf(
                        "cash",
                        "bank_transfer",
                        "card_manual",
                        "card_gateway",
                        "credit",
                        "mixed",
                        "other"
                    )
                )
            )
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "draft",
                        "pending",
                        "confirmed",
                        "allocated",
                        "reversed",
                        "voided",
                        "failed"
                    )
                )
            )
            .append("amount", MongoMigrationSupport.moneyObject())
            .append("paidAt", MongoMigrationSupport.date())
            .append("externalReference", MongoMigrationSupport.nullableString(maxLength = 256))
            .append("allocations", MongoMigrationSupport.array())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.PAYMENTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "method",
                    "status",
                    "amount",
                    "paidAt"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "paidAt"),
            "payments_org_branch_paid_at_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "saleId", "status"),
            "payments_org_sale_status_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "cashSessionId", "status"),
            "payments_org_cash_session_status_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "method", "paidAt"),
            "payments_org_method_paid_at_idx"
        )
    }

    private fun createReceivables(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("saleId", MongoMigrationSupport.id(prefix = "sale_"))
            .append("customerId", MongoMigrationSupport.id(prefix = "cus_"))
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "open",
                        "partially_collected",
                        "settled",
                        "overdue",
                        "written_off",
                        "canceled"
                    )
                )
            )
            .append("originalAmount", MongoMigrationSupport.moneyObject())
            .append("paidAmount", MongoMigrationSupport.moneyObject())
            .append("balanceDue", MongoMigrationSupport.moneyObject())
            .append("dueAt", MongoMigrationSupport.nullableDate())
            .append("settledAt", MongoMigrationSupport.nullableDate())
            .append("paymentRefs", MongoMigrationSupport.array())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.RECEIVABLES,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "saleId",
                    "customerId",
                    "status",
                    "originalAmount",
                    "paidAmount",
                    "balanceDue"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "customerId", "status", "dueAt"),
            "receivables_org_customer_status_due_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "saleId"),
            "receivables_org_sale_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "dueAt"),
            "receivables_org_status_due_idx"
        )
    }
}
