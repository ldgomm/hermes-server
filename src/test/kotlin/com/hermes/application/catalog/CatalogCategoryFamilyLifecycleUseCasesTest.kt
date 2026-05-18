package com.hermes.application.catalog

import com.hermes.domain.catalog.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogCategoryFamilyLifecycleUseCasesTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val adminPermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER)

    @Test
    fun `creates category and family with normalized searchable metadata`() {
        val fixture = fixture()

        val category = fixture.createCategory.execute(
            CatalogCreateCategoryCommand(
                actorUserId = "usr_admin",
                actorEffectivePermissions = adminPermissions,
                code = "Restaurant Main Dishes",
                name = "Platos Fuertes",
                businessTypeTags = setOf("Restaurant", "Food_Service"),
                activityTags = setOf("Food_Service"),
            )
        ).category

        val family = fixture.createFamily.execute(
            CatalogCreateFamilyCommand(
                actorUserId = "usr_admin",
                actorEffectivePermissions = adminPermissions,
                globalFamilyId = "Cuy Preparado",
                canonicalName = "Cuy preparado",
                categoryId = category.id,
                type = CatalogItemType.PRODUCT,
                aliases = listOf("cuy", "cuy asado", "cuy"),
            )
        ).family

        assertEquals("restaurant_main_dishes", category.code)
        assertEquals("platos fuertes", category.normalizedName)
        assertEquals("cuy_preparado", family.globalFamilyId)
        assertEquals(listOf("cuy", "cuy asado"), family.aliases)
        assertEquals(2, fixture.audit.events.size)
    }

    @Test
    fun `rejects family linked to inactive category`() {
        val fixture = fixture()
        val inactive = CatalogCategory(
            id = "cat_inactive",
            code = "inactive",
            name = "Inactive",
            status = CatalogCategoryStatus.PAUSED,
            createdAt = now,
            updatedAt = now,
        )
        fixture.categories.create(inactive)

        assertFailsWith<DomainRuleViolation> {
            fixture.createFamily.execute(
                CatalogCreateFamilyCommand(
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = adminPermissions,
                    globalFamilyId = "family_1",
                    canonicalName = "Family",
                    categoryId = inactive.id,
                )
            )
        }
    }

    @Test
    fun `updates template family and archives without allowing reactivation`() {
        val fixture = fixture()
        fixture.categories.create(
            CatalogCategory(
                id = "cat_food",
                code = "food",
                name = "Food",
                createdAt = now,
                updatedAt = now
            )
        )
        fixture.families.create(
            PlatformCatalogFamily(
                id = "pfam_cuy",
                globalFamilyId = "cuy",
                canonicalName = "Cuy",
                categoryId = "cat_food",
                type = CatalogItemType.PRODUCT,
                createdAt = now,
                updatedAt = now,
            )
        )
        fixture.templates.create(
            PlatformCatalogTemplate(
                id = "tpl_cuy_entero",
                globalCatalogId = "cuy_entero",
                canonicalName = "Cuy entero",
                normalizedName = "cuy entero",
                type = CatalogItemType.PRODUCT,
                status = CatalogTemplateStatus.DRAFT,
            )
        )

        val updated = fixture.updateTemplate.execute(
            CatalogUpdateTemplateCommand(
                actorUserId = "usr_admin",
                actorEffectivePermissions = adminPermissions,
                templateId = "tpl_cuy_entero",
                canonicalName = "Cuy entero asado",
                productFamilyId = "pfam_cuy",
                reason = "Asociar familia",
            )
        ).template
        assertEquals("pfam_cuy", updated.productFamilyId)

        val published = fixture.changeTemplateStatus.execute(
            CatalogChangeTemplateStatusCommand(
                actorUserId = "usr_admin",
                actorEffectivePermissions = adminPermissions,
                templateId = "tpl_cuy_entero",
                status = CatalogTemplateStatus.ACTIVE,
                reason = "Publicar",
            )
        ).template
        assertEquals(CatalogTemplateStatus.ACTIVE, published.status)

        fixture.changeTemplateStatus.execute(
            CatalogChangeTemplateStatusCommand(
                actorUserId = "usr_admin",
                actorEffectivePermissions = adminPermissions,
                templateId = "tpl_cuy_entero",
                status = CatalogTemplateStatus.ARCHIVED,
                reason = "Archivar",
            )
        )

        assertFailsWith<DomainRuleViolation> {
            fixture.changeTemplateStatus.execute(
                CatalogChangeTemplateStatusCommand(
                    actorUserId = "usr_admin",
                    actorEffectivePermissions = adminPermissions,
                    templateId = "tpl_cuy_entero",
                    status = CatalogTemplateStatus.ACTIVE,
                    reason = "No debe reactivar",
                )
            )
        }
        assertTrue(fixture.audit.events.any { it.action == CatalogAuditAction.PLATFORM_TEMPLATE_ARCHIVED })
    }

    private fun fixture(): Fixture {
        val categories = InMemoryCategoryRepository()
        val families = InMemoryFamilyRepository()
        val templates = InMemoryTemplateRepository()
        val audit = RecordingCatalogAuditLogger()
        val idGenerator = CatalogIdGenerator { prefix -> "${prefix}_${idSequence++}" }

        return Fixture(
            categories = categories,
            families = families,
            templates = templates,
            audit = audit,
            createCategory = CatalogCreateCategoryUseCase(categories, idGenerator, audit, clock),
            createFamily = CatalogCreateFamilyUseCase(families, categories, idGenerator, audit, clock),
            updateTemplate = CatalogUpdateTemplateUseCase(templates, families, audit, clock),
            changeTemplateStatus = CatalogChangeTemplateStatusUseCase(templates, audit, clock),
        )
    }

    private data class Fixture(
        val categories: InMemoryCategoryRepository,
        val families: InMemoryFamilyRepository,
        val templates: InMemoryTemplateRepository,
        val audit: RecordingCatalogAuditLogger,
        val createCategory: CatalogCreateCategoryUseCase,
        val createFamily: CatalogCreateFamilyUseCase,
        val updateTemplate: CatalogUpdateTemplateUseCase,
        val changeTemplateStatus: CatalogChangeTemplateStatusUseCase,
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
        override fun search(query: CatalogCategorySearchQuery): List<CatalogCategory> = items.values.toList()
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

        override fun search(query: PlatformCatalogFamilySearchQuery): List<PlatformCatalogFamily> =
            items.values.toList()
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
            items.values.any { it.globalCatalogId == globalCatalogId }

        override fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate> = items.values.toList()
    }

    private class RecordingCatalogAuditLogger : CatalogAuditLogger {
        val events = mutableListOf<CatalogAuditEvent>()
        override fun log(event: CatalogAuditEvent) {
            events += event
        }
    }

    private companion object {
        var idSequence: Int = 1
    }
}
