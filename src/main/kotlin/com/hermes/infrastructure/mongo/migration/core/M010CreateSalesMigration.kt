package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M010CreateSalesMigration : MongoMigration {
    override val id: String = "M010_create_sales"
    override val description: String =
        "Create sales collection with separated operational, payment and document states."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("activityId", MongoMigrationSupport.id(prefix = "act_"))
            .append("saleNumber", MongoMigrationSupport.string(maxLength = 64))
            .append(
                "saleType",
                MongoMigrationSupport.enum(
                    listOf(
                        "standard_sale",
                        "internal_order",
                        "service_order",
                        "reservation_sale",
                        "rental_sale",
                        "quote"
                    )
                )
            )
            .append(
                "workflowMode",
                MongoMigrationSupport.enum(
                    listOf(
                        "quick_sale",
                        "table_order",
                        "service_order",
                        "appointment",
                        "reservation",
                        "rental",
                        "quote_to_sale",
                        "delivery_order",
                        "counter_order"
                    )
                )
            )
            .append(
                "operationalStatus",
                MongoMigrationSupport.enum(
                    listOf(
                        "draft",
                        "pending",
                        "confirmed",
                        "in_progress",
                        "ready",
                        "delivered",
                        "closed",
                        "canceled",
                        "voided"
                    )
                )
            )
            .append(
                "paymentStatus",
                MongoMigrationSupport.enum(listOf("unpaid", "partially_paid", "paid", "overpaid", "refunded", "voided"))
            )
            .append(
                "collectionStatus",
                MongoMigrationSupport.enum(
                    listOf(
                        "not_applicable",
                        "pending_receivable",
                        "partially_collected",
                        "settled",
                        "overdue",
                        "written_off"
                    )
                )
            )
            .append(
                "documentStatus",
                MongoMigrationSupport.enum(
                    listOf(
                        "not_required",
                        "draft",
                        "generated",
                        "validated",
                        "signed",
                        "sent",
                        "received",
                        "authorized",
                        "rejected",
                        "returned",
                        "cancellation_requested",
                        "pending_cancellation",
                        "canceled",
                        "error"
                    )
                )
            )
            .append("customerId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("customerSnapshot", MongoMigrationSupport.obj())
            .append("items", MongoMigrationSupport.array())
            .append("totals", MongoMigrationSupport.obj())
            .append("taxSummary", MongoMigrationSupport.obj())
            .append("paymentRefs", MongoMigrationSupport.array())
            .append("documentRefs", MongoMigrationSupport.array())
            .append("reservationRef", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("cashSessionId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("confirmedAt", MongoMigrationSupport.nullableDate())
            .append("closedAt", MongoMigrationSupport.nullableDate())
            .append("canceledAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.SALES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId", "activityId", "saleNumber", "saleType", "workflowMode", "operationalStatus",
                    "paymentStatus", "collectionStatus", "documentStatus", "items", "totals", "taxSummary",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "saleNumber"),
            "sales_org_number_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "createdAt"),
            "sales_org_branch_created_at_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "activityId", "operationalStatus", "createdAt"),
            "sales_org_activity_status_created_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "paymentStatus", "createdAt"),
            "sales_org_payment_status_created_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "documentStatus", "createdAt"),
            "sales_org_document_status_created_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "customerId", "createdAt"),
            "sales_org_customer_created_idx",
            sparse = true
        )
    }
}
