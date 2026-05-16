package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M011CreateServicesReservationsMigration : MongoMigration {
    override val id: String = "M011_create_services_reservations"
    override val description: String = "Create service order, reservation, slot and capacity resource collections."

    override fun up(database: MongoDatabase) {
        createServiceOrders(database)
        createReservations(database)
        createReservationSlots(database)
        createCapacityResources(database)
    }

    private fun createServiceOrders(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("saleId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("activityId", MongoMigrationSupport.id(prefix = "act_"))
            .append("customerId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "draft",
                        "pending",
                        "confirmed",
                        "in_progress",
                        "ready",
                        "delivered",
                        "closed",
                        "canceled"
                    )
                )
            )
            .append("scheduledStartAt", MongoMigrationSupport.nullableDate())
            .append("scheduledEndAt", MongoMigrationSupport.nullableDate())
            .append("metadata", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.SERVICE_ORDERS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "activityId",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "status", "scheduledStartAt"),
            "service_orders_org_branch_status_start_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "saleId"),
            "service_orders_org_sale_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "customerId", "scheduledStartAt"),
            "service_orders_org_customer_start_idx",
            sparse = true
        )
    }

    private fun createReservations(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("saleId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("activityId", MongoMigrationSupport.id(prefix = "act_"))
            .append("customerId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("resourceId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("startAt", MongoMigrationSupport.date())
            .append("endAt", MongoMigrationSupport.date())
            .append("partySize", MongoMigrationSupport.int())
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "draft",
                        "pending",
                        "confirmed",
                        "in_progress",
                        "completed",
                        "no_show",
                        "canceled",
                        "expired"
                    )
                )
            )
            .append("notes", MongoMigrationSupport.nullableString(maxLength = 2048))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.RESERVATIONS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "activityId",
                    "startAt",
                    "endAt",
                    "partySize",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "activityId", "startAt"),
            "reservations_org_branch_activity_start_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "startAt"),
            "reservations_org_status_start_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "resourceId", "startAt", "endAt"),
            "reservations_org_resource_time_idx",
            sparse = true
        )
    }

    private fun createReservationSlots(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("activityId", MongoMigrationSupport.id(prefix = "act_"))
            .append("resourceId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("startAt", MongoMigrationSupport.date())
            .append("endAt", MongoMigrationSupport.date())
            .append("capacity", MongoMigrationSupport.int())
            .append("reservedCount", MongoMigrationSupport.int())
            .append("status", MongoMigrationSupport.enum(listOf("open", "full", "blocked", "closed", "canceled")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.RESERVATION_SLOTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "activityId",
                    "startAt",
                    "endAt",
                    "capacity",
                    "reservedCount",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "activityId", "startAt"),
            "reservation_slots_org_branch_activity_start_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "resourceId", "startAt"),
            "reservation_slots_org_resource_start_idx",
            sparse = true
        )
    }

    private fun createCapacityResources(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("activityId", MongoMigrationSupport.id(prefix = "act_"))
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "type",
                MongoMigrationSupport.enum(
                    listOf(
                        "person_capacity",
                        "table",
                        "vehicle",
                        "room",
                        "equipment",
                        "generic"
                    )
                )
            )
            .append("capacity", MongoMigrationSupport.int())
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "maintenance", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CAPACITY_RESOURCES,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "activityId",
                    "name",
                    "type",
                    "capacity",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "activityId", "status"),
            "capacity_resources_org_branch_activity_status_idx"
        )
    }
}
