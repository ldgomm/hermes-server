package com.hermes.infrastructure.mongo.catalog

import com.hermes.application.catalog.CatalogCategorySearchQuery
import com.hermes.application.catalog.PlatformCatalogFamilySearchQuery
import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.PlatformCatalogFamily
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.core.M005CreatePlatformCatalogMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MongoCatalogCategoryFamilyStoreIntegrationTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeEach
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("hermes_catalog_category_family_test")
        val database = client.getDatabase(databaseName)
        M005CreatePlatformCatalogMigration.up(database)
        CatalogMongoBootstrap.ensureIndexes(database)
    }

    @AfterEach
    fun tearDown() {
        if (::client.isInitialized) {
            runCatching { client.getDatabase(databaseName).drop() }
            runCatching { client.close() }
        }
    }

    @Test
    fun `persists and searches categories and families`() {
        val store = MongoCatalogCategoryFamilyStore(client.getDatabase(databaseName))
        val now = Instant.parse("2026-05-17T12:00:00Z")

        val category = CatalogCategory(
            id = "cat_restaurant_main_dishes",
            parentId = "cat_restaurant",
            code = "restaurant_main_dishes",
            name = "Platos fuertes",
            normalizedName = "platos fuertes",
            businessTypeTags = setOf("restaurant"),
            activityTags = setOf("food_service"),
            sortOrder = 10,
            createdAt = now,
            updatedAt = now,
        )
        store.categoryRepository.create(category)

        val family = PlatformCatalogFamily(
            id = "pfam_cuy",
            globalFamilyId = "cuy_preparado",
            canonicalName = "Cuy preparado",
            normalizedName = "cuy preparado",
            categoryId = category.id,
            type = CatalogItemType.PRODUCT,
            aliases = listOf("cuy", "cuy asado"),
            createdAt = now,
            updatedAt = now,
        )
        store.familyRepository.create(family)

        assertEquals(category.id, store.categoryRepository.findByCode("restaurant_main_dishes")?.id)
        assertEquals(family.id, store.familyRepository.findByGlobalFamilyId("cuy_preparado")?.id)
        assertEquals(1, store.categoryRepository.search(CatalogCategorySearchQuery(query = "platos")).size)
        assertEquals(
            1,
            store.familyRepository.search(
                PlatformCatalogFamilySearchQuery(
                    query = "cuy",
                    categoryId = category.id
                )
            ).size
        )
    }

    @Test
    fun `installs category and family indexes`() {
        val database = client.getDatabase(databaseName)
        assertIndexes(
            database.getCollection(MongoCollectionNames.PLATFORM_CATEGORIES).listIndexes().map { it.getString("name") }
                .toSet(),
            setOf(
                "platform_categories_code_unique_idx",
                "platform_categories_parent_status_sort_idx",
                "platform_categories_status_sort_idx",
                "platform_categories_normalized_name_idx",
            ),
        )
        assertIndexes(
            database.getCollection(MongoCollectionNames.PLATFORM_CATALOG_FAMILIES).listIndexes()
                .map { it.getString("name") }.toSet(),
            setOf(
                "platform_catalog_families_global_id_unique_idx",
                "platform_catalog_families_category_status_idx",
                "platform_catalog_families_type_status_idx",
                "platform_catalog_families_normalized_name_idx",
            ),
        )
    }

    private fun assertIndexes(existing: Set<String>, required: Set<String>) {
        required.forEach { indexName ->
            assertTrue(indexName in existing, "Missing Mongo index: $indexName")
        }
    }
}
