package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogCategoryStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.PlatformCatalogFamily
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogInitialSeedUseCaseTest {
    private val now = Instant.parse("2026-05-18T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val adminPermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER)

    @Test
    fun `seeds restaurant tourism and retail initial catalog`() {
        val fixture = fixture()

        val result = fixture.seed.execute(
            CatalogInitialSeedCommand(
                actorUserId = "usr_platform_admin",
                actorEffectivePermissions = adminPermissions,
                reason = "Seed piloto",
            )
        )

        assertEquals(48, result.total)
        assertEquals(48, result.created)
        assertEquals(0, result.updated)
        assertEquals(0, result.unchanged)
        assertNotNull(fixture.categories.findByCode("restaurant_main_dishes"))
        assertNotNull(fixture.families.findByGlobalFamilyId("tourism_offroad"))
        assertTrue(fixture.templates.existsByGlobalCatalogId("restaurant_cuy_entero"))
        assertTrue(fixture.templates.existsByGlobalCatalogId("tourism_offroad_1h"))
        assertTrue(fixture.templates.existsByGlobalCatalogId("retail_water_500ml"))
        assertTrue(fixture.audit.events.any { it.action == CatalogAuditAction.PLATFORM_TEMPLATE_CREATED })
    }

    @Test
    fun `seed is idempotent`() {
        val fixture = fixture()

        fixture.seed.execute(
            CatalogInitialSeedCommand(
                actorUserId = "usr_platform_admin",
                actorEffectivePermissions = adminPermissions,
            )
        )

        val secondRun = fixture.seed.execute(
            CatalogInitialSeedCommand(
                actorUserId = "usr_platform_admin",
                actorEffectivePermissions = adminPermissions,
            )
        )

        assertEquals(48, secondRun.total)
        assertEquals(0, secondRun.created)
        assertEquals(0, secondRun.updated)
        assertEquals(48, secondRun.unchanged)
        assertEquals(14, fixture.categories.items.size)
        assertEquals(12, fixture.families.items.size)
        assertEquals(22, fixture.templates.items.size)
    }

    @Test
    fun `can seed only restaurant vertical`() {
        val fixture = fixture()

        val result = fixture.seed.execute(
            CatalogInitialSeedCommand(
                actorUserId = "usr_platform_admin",
                actorEffectivePermissions = adminPermissions,
                verticals = setOf(CatalogInitialSeedVertical.RESTAURANT),
            )
        )

        assertEquals(21, result.total)
        assertEquals(21, result.created)
        assertNotNull(fixture.categories.findByCode("restaurant_main_dishes"))
        assertTrue(fixture.templates.existsByGlobalCatalogId("restaurant_yahuarlocro"))
        assertEquals(false, fixture.templates.existsByGlobalCatalogId("tourism_offroad_1h"))
    }

    @Test
    fun `updates stale seeded category while keeping same id`() {
        val fixture = fixture()
        fixture.categories.create(
            CatalogCategory(
                id = "cat_existing_restaurant",
                code = "restaurant",
                name = "Old restaurant",
                normalizedName = "old restaurant",
                status = CatalogCategoryStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        )

        val result = fixture.seed.execute(
            CatalogInitialSeedCommand(
                actorUserId = "usr_platform_admin",
                actorEffectivePermissions = adminPermissions,
                verticals = setOf(CatalogInitialSeedVertical.RESTAURANT),
            )
        )

        val updated = fixture.categories.findByCode("restaurant")
        assertEquals("cat_existing_restaurant", updated?.id)
        assertEquals("Restaurante", updated?.name)
        assertEquals(1, result.items.count { it.outcome == CatalogSeedOutcome.UPDATED })
    }

    @Test
    fun `rejects seed without master catalog permission`() {
        val fixture = fixture()

        assertFailsWith<DomainRuleViolation> {
            fixture.seed.execute(
                CatalogInitialSeedCommand(
                    actorUserId = "usr_operator",
                    actorEffectivePermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
                )
            )
        }
    }

    private fun fixture(): Fixture {
        val categories = InMemoryCategoryRepository()
        val families = InMemoryFamilyRepository()
        val templates = InMemoryTemplateRepository()
        val audit = RecordingCatalogAuditLogger()
        return Fixture(
            categories = categories,
            families = families,
            templates = templates,
            audit = audit,
            seed = SeedInitialCatalogUseCase(
                categoryRepository = categories,
                familyRepository = families,
                templateRepository = templates,
                auditLogger = audit,
                clock = clock,
            ),
        )
    }

    private data class Fixture(
        val categories: InMemoryCategoryRepository,
        val families: InMemoryFamilyRepository,
        val templates: InMemoryTemplateRepository,
        val audit: RecordingCatalogAuditLogger,
        val seed: SeedInitialCatalogUseCase,
    )

    private class InMemoryCategoryRepository : CatalogCategoryRepository {
        val items = linkedMapOf<String, CatalogCategory>()

        override fun create(category: CatalogCategory) {
            items[category.id] = category
        }

        override fun update(category: CatalogCategory) {
            items[category.id] = category
        }

        override fun findById(id: String): CatalogCategory? = items[id]

        override fun findByCode(code: String): CatalogCategory? =
            items.values.firstOrNull { it.code == code.trim().lowercase() }

        override fun existsByCode(code: String): Boolean = findByCode(code) != null

        override fun search(query: CatalogCategorySearchQuery): List<CatalogCategory> = items.values
            .filter { query.parentId == null || it.parentId == query.parentId }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }
            .filter { query.query.isNullOrBlank() || it.name.contains(query.query, ignoreCase = true) || it.code.contains(query.query, ignoreCase = true) }
            .take(query.limit)
    }

    private class InMemoryFamilyRepository : PlatformCatalogFamilyRepository {
        val items = linkedMapOf<String, PlatformCatalogFamily>()

        override fun create(family: PlatformCatalogFamily) {
            items[family.id] = family
        }

        override fun update(family: PlatformCatalogFamily) {
            items[family.id] = family
        }

        override fun findById(id: String): PlatformCatalogFamily? = items[id]

        override fun findByGlobalFamilyId(globalFamilyId: String): PlatformCatalogFamily? =
            items.values.firstOrNull { it.globalFamilyId == globalFamilyId.trim().lowercase() }

        override fun existsByGlobalFamilyId(globalFamilyId: String): Boolean =
            findByGlobalFamilyId(globalFamilyId) != null

        override fun search(query: PlatformCatalogFamilySearchQuery): List<PlatformCatalogFamily> = items.values
            .filter { query.categoryId == null || it.categoryId == query.categoryId }
            .filter { query.type == null || it.type == query.type }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }
            .filter { query.query.isNullOrBlank() || it.canonicalName.contains(query.query, ignoreCase = true) || it.globalFamilyId.contains(query.query, ignoreCase = true) }
            .take(query.limit)
    }

    private class InMemoryTemplateRepository : PlatformCatalogTemplateRepository {
        val items = linkedMapOf<String, PlatformCatalogTemplate>()

        override fun create(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        override fun update(template: PlatformCatalogTemplate) {
            items[template.id] = template
        }

        override fun findById(id: String): PlatformCatalogTemplate? = items[id]

        override fun existsByGlobalCatalogId(globalCatalogId: String): Boolean =
            items.values.any { it.globalCatalogId == globalCatalogId.trim().lowercase() }

        override fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate> = items.values
            .filter { !query.onlyActive || it.status == CatalogTemplateStatus.ACTIVE }
            .filter { query.type == null || it.type == query.type }
            .filter { query.identifier.isNullOrBlank() || it.identifiers.any { identifier -> identifier.normalizedValue == query.identifier } }
            .filter { query.query.isNullOrBlank() || it.globalCatalogId.contains(query.query, ignoreCase = true) || it.canonicalName.contains(query.query, ignoreCase = true) }
            .take(query.limit)
    }

    private class RecordingCatalogAuditLogger : CatalogAuditLogger {
        val events = mutableListOf<CatalogAuditEvent>()
        override fun log(event: CatalogAuditEvent) {
            events += event
        }
    }
}
