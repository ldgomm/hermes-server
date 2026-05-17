package com.hermes.infrastructure.mongo.catalog

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.Document
import org.bson.conversions.Bson

object CatalogMongoBootstrap {
    fun ensureIndexes(database: MongoDatabase) {
        ensurePlatformTemplateIndexes(database)
        ensureOrganizationCatalogItemIndexes(database)
        ensureCatalogRequestIndexes(database)
        ensureCatalogPriceHistoryIndexes(database)
        ensureCatalogAuditIndexes(database)
    }

    private fun ensurePlatformTemplateIndexes(database: MongoDatabase) {
        val collection = database.getCollection(MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES)

        collection.createIndexIfMissing(
            keys = Indexes.ascending("globalCatalogId"),
            name = "platform_catalog_templates_global_id_unique_idx",
            unique = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("productFamilyId", "status"),
            name = "platform_catalog_templates_family_status_idx",
            sparse = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("type", "status"),
            name = "platform_catalog_templates_type_status_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("normalizedName"),
            name = "platform_catalog_templates_normalized_name_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("identifiers.normalizedValue"),
            name = "platform_catalog_templates_identifier_idx",
            sparse = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("attributes.businessTypeTags"),
            name = "platform_catalog_templates_business_type_tags_idx",
            sparse = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("attributes.activityTags"),
            name = "platform_catalog_templates_activity_tags_idx",
            sparse = true,
        )
    }

    private fun ensureOrganizationCatalogItemIndexes(database: MongoDatabase) {
        val collection = database.getCollection(MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS)

        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "status"),
            name = "organization_catalog_items_org_status_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "branchId", "status"),
            name = "organization_catalog_items_org_branch_status_idx",
            sparse = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "activityId", "status"),
            name = "organization_catalog_items_org_activity_status_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "searchableText"),
            name = "organization_catalog_items_org_searchable_text_ascending_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "identifiers.normalizedValue"),
            name = "organization_catalog_items_org_identifier_idx",
            sparse = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "globalCatalogId"),
            name = "organization_catalog_items_org_global_id_idx",
            sparse = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "templateId"),
            name = "organization_catalog_items_org_template_idx",
            sparse = true,
        )
    }

    private fun ensureCatalogRequestIndexes(database: MongoDatabase) {
        val collection = database.getCollection(MongoCollectionNames.CATALOG_ITEM_REQUESTS)

        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "status"),
            name = "catalog_item_requests_org_status_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("requestedByUserId", "status"),
            name = "catalog_item_requests_requested_by_status_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "normalizedRequestedName", "status"),
            name = "catalog_item_requests_org_normalized_name_status_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("status", "createdAt"),
            name = "catalog_item_requests_status_created_at_idx",
        )
    }

    private fun ensureCatalogPriceHistoryIndexes(database: MongoDatabase) {
        val collection = database.getCollection(MongoCollectionNames.CATALOG_PRICE_HISTORY)

        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "catalogItemId", "changedAt"),
            name = "catalog_price_history_org_item_changed_at_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("organizationId", "changedAt"),
            name = "catalog_price_history_org_changed_at_idx",
        )
    }

    private fun ensureCatalogAuditIndexes(database: MongoDatabase) {
        val collection = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

        collection.createIndexIfMissing(
            keys = Indexes.ascending("module", "organizationId", "createdAt"),
            name = "audit_logs_module_org_created_at_idx",
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("module", "targetId", "createdAt"),
            name = "audit_logs_module_target_created_at_idx",
            sparse = true,
        )
        collection.createIndexIfMissing(
            keys = Indexes.ascending("module", "actorUserId", "createdAt"),
            name = "audit_logs_module_actor_created_at_idx",
            sparse = true,
        )
    }

    private fun MongoCollection<Document>.createIndexIfMissing(
        keys: Bson,
        name: String,
        unique: Boolean = false,
        sparse: Boolean = false,
    ) {
        val existingNames = listIndexes()
            .map { it.getString("name") }
            .filterNotNull()
            .toSet()

        if (name in existingNames) return

        createIndex(
            keys,
            IndexOptions()
                .name(name)
                .unique(unique)
                .sparse(sparse),
        )
    }
}
