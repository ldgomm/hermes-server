package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogCategoryStatus
import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogIdentifierScope
import com.hermes.domain.catalog.CatalogIdentifierSource
import com.hermes.domain.catalog.CatalogIdentifierStatus
import com.hermes.domain.catalog.CatalogIdentifierType
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.PlatformCatalogFamily
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

/**
 * Fase 7.8 — Initial governed catalog seed for pilot verticals.
 *
 * The seed is deliberately small and operational: enough to pilot restaurant,
 * tourism/experiences, and a tiny retail/corner-shop catalog without pretending
 * to be a complete ERP catalog.
 */
class SeedInitialCatalogUseCase(
    private val categoryRepository: CatalogCategoryRepository,
    private val familyRepository: PlatformCatalogFamilyRepository,
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogInitialSeedCommand): CatalogInitialSeedResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)

        val now = Instant.now(clock)
        val verticals = command.verticals.ifEmpty { enumValues<CatalogInitialSeedVertical>().toSet() }
        val results = mutableListOf<CatalogSeedItemResult>()

        val categoryDefinitions = CatalogInitialSeedDefinitions.categories
            .filter { it.vertical in verticals }
        val familyDefinitions = CatalogInitialSeedDefinitions.families
            .filter { it.vertical in verticals }
        val templateDefinitions = CatalogInitialSeedDefinitions.templates
            .filter { it.vertical in verticals }

        val categoryIdsByCode = mutableMapOf<String, String>()
        categoryDefinitions.forEach { definition ->
            val parentId = definition.parentCode?.let { parentCode ->
                categoryIdsByCode[parentCode]
                    ?: categoryRepository.findByCode(parentCode)?.id
                    ?: throw DomainRuleViolation("Parent seed category does not exist: $parentCode.")
            }
            val result = upsertCategory(definition, parentId, command, now)
            categoryIdsByCode[definition.code] = result.id
            results += result
        }

        val familyIdsByGlobalId = mutableMapOf<String, String>()
        familyDefinitions.forEach { definition ->
            val categoryId = definition.categoryCode?.let { categoryCode ->
                categoryIdsByCode[categoryCode]
                    ?: categoryRepository.findByCode(categoryCode)?.id
                    ?: throw DomainRuleViolation("Seed family category does not exist: $categoryCode.")
            }
            val result = upsertFamily(definition, categoryId, command, now)
            familyIdsByGlobalId[definition.globalFamilyId] = result.id
            results += result
        }

        templateDefinitions.forEach { definition ->
            val familyId = definition.familyGlobalId?.let { globalFamilyId ->
                familyIdsByGlobalId[globalFamilyId]
                    ?: familyRepository.findByGlobalFamilyId(globalFamilyId)?.id
                    ?: throw DomainRuleViolation("Seed template family does not exist: $globalFamilyId.")
            }
            results += upsertTemplate(definition, familyId, command, now)
        }

        return CatalogInitialSeedResult(
            verticals = verticals,
            items = results,
        )
    }

    private fun upsertCategory(
        definition: CatalogSeedCategoryDefinition,
        parentId: String?,
        command: CatalogInitialSeedCommand,
        now: Instant,
    ): CatalogSeedItemResult {
        val existing = categoryRepository.findByCode(definition.code)
        val desired = CatalogCategory(
            id = existing?.id ?: definition.id,
            parentId = parentId,
            code = definition.code,
            name = definition.name,
            normalizedName = definition.name.normalizedSearchText(),
            description = definition.description,
            businessTypeTags = definition.businessTypeTags,
            activityTags = definition.activityTags,
            status = CatalogCategoryStatus.ACTIVE,
            sortOrder = definition.sortOrder,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            version = existing?.version ?: 1,
        )

        return when {
            existing == null -> {
                categoryRepository.create(desired)
                auditLogger.log(
                    CatalogAuditEvent(
                        action = CatalogAuditAction.PLATFORM_CATEGORY_CREATED,
                        actorUserId = command.actorUserId,
                        organizationId = null,
                        targetId = desired.id,
                        after = mapOf("code" to desired.code, "name" to desired.name, "seed" to "initial"),
                        reason = command.reason,
                        createdAt = now,
                    )
                )
                CatalogSeedItemResult(CatalogSeedEntityType.CATEGORY, desired.code, desired.id, CatalogSeedOutcome.CREATED)
            }

            existing.seedEquivalentTo(desired) ->
                CatalogSeedItemResult(CatalogSeedEntityType.CATEGORY, existing.code, existing.id, CatalogSeedOutcome.UNCHANGED)

            else -> {
                val updated = desired.copy(version = existing.version + 1)
                categoryRepository.update(updated)
                auditLogger.log(
                    CatalogAuditEvent(
                        action = CatalogAuditAction.PLATFORM_CATEGORY_UPDATED,
                        actorUserId = command.actorUserId,
                        organizationId = null,
                        targetId = updated.id,
                        before = mapOf("code" to existing.code, "name" to existing.name, "status" to existing.status.name),
                        after = mapOf("code" to updated.code, "name" to updated.name, "status" to updated.status.name, "seed" to "initial"),
                        reason = command.reason,
                        createdAt = now,
                    )
                )
                CatalogSeedItemResult(CatalogSeedEntityType.CATEGORY, updated.code, updated.id, CatalogSeedOutcome.UPDATED)
            }
        }
    }

    private fun upsertFamily(
        definition: CatalogSeedFamilyDefinition,
        categoryId: String?,
        command: CatalogInitialSeedCommand,
        now: Instant,
    ): CatalogSeedItemResult {
        val existing = familyRepository.findByGlobalFamilyId(definition.globalFamilyId)
        val desired = PlatformCatalogFamily(
            id = existing?.id ?: definition.id,
            globalFamilyId = definition.globalFamilyId,
            canonicalName = definition.canonicalName,
            normalizedName = definition.canonicalName.normalizedSearchText(),
            categoryId = categoryId,
            brand = definition.brand,
            type = definition.type,
            aliases = definition.aliases,
            attributes = definition.attributes,
            status = CatalogTemplateStatus.ACTIVE,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            version = existing?.version ?: 1,
        )

        return when {
            existing == null -> {
                familyRepository.create(desired)
                auditLogger.log(
                    CatalogAuditEvent(
                        action = CatalogAuditAction.PLATFORM_FAMILY_CREATED,
                        actorUserId = command.actorUserId,
                        organizationId = null,
                        targetId = desired.id,
                        after = mapOf("globalFamilyId" to desired.globalFamilyId, "canonicalName" to desired.canonicalName, "seed" to "initial"),
                        reason = command.reason,
                        createdAt = now,
                    )
                )
                CatalogSeedItemResult(CatalogSeedEntityType.FAMILY, desired.globalFamilyId, desired.id, CatalogSeedOutcome.CREATED)
            }

            existing.seedEquivalentTo(desired) ->
                CatalogSeedItemResult(CatalogSeedEntityType.FAMILY, existing.globalFamilyId, existing.id, CatalogSeedOutcome.UNCHANGED)

            else -> {
                val updated = desired.copy(version = existing.version + 1)
                familyRepository.update(updated)
                auditLogger.log(
                    CatalogAuditEvent(
                        action = CatalogAuditAction.PLATFORM_FAMILY_UPDATED,
                        actorUserId = command.actorUserId,
                        organizationId = null,
                        targetId = updated.id,
                        before = mapOf("globalFamilyId" to existing.globalFamilyId, "canonicalName" to existing.canonicalName, "status" to existing.status.name),
                        after = mapOf("globalFamilyId" to updated.globalFamilyId, "canonicalName" to updated.canonicalName, "status" to updated.status.name, "seed" to "initial"),
                        reason = command.reason,
                        createdAt = now,
                    )
                )
                CatalogSeedItemResult(CatalogSeedEntityType.FAMILY, updated.globalFamilyId, updated.id, CatalogSeedOutcome.UPDATED)
            }
        }
    }

    private fun upsertTemplate(
        definition: CatalogSeedTemplateDefinition,
        familyId: String?,
        command: CatalogInitialSeedCommand,
        now: Instant,
    ): CatalogSeedItemResult {
        val existing = templateRepository.findByGlobalCatalogId(definition.globalCatalogId)
        val desired = PlatformCatalogTemplate(
            id = existing?.id ?: definition.id,
            globalCatalogId = definition.globalCatalogId,
            canonicalName = definition.canonicalName,
            normalizedName = definition.canonicalName.normalizedSearchText(),
            type = definition.type,
            status = CatalogTemplateStatus.ACTIVE,
            productFamilyId = familyId,
            variantAttributes = definition.variantAttributes,
            identifiers = definition.identifiers,
            attributes = definition.attributes,
        )

        return when {
            existing == null -> {
                templateRepository.create(desired)
                auditLogger.log(
                    CatalogAuditEvent(
                        action = CatalogAuditAction.PLATFORM_TEMPLATE_CREATED,
                        actorUserId = command.actorUserId,
                        organizationId = null,
                        targetId = desired.id,
                        after = mapOf("globalCatalogId" to desired.globalCatalogId, "canonicalName" to desired.canonicalName, "seed" to "initial"),
                        reason = command.reason,
                        createdAt = now,
                    )
                )
                CatalogSeedItemResult(CatalogSeedEntityType.TEMPLATE, desired.globalCatalogId, desired.id, CatalogSeedOutcome.CREATED)
            }

            existing.seedEquivalentTo(desired) ->
                CatalogSeedItemResult(CatalogSeedEntityType.TEMPLATE, existing.globalCatalogId, existing.id, CatalogSeedOutcome.UNCHANGED)

            else -> {
                templateRepository.update(desired)
                auditLogger.log(
                    CatalogAuditEvent(
                        action = CatalogAuditAction.PLATFORM_TEMPLATE_UPDATED,
                        actorUserId = command.actorUserId,
                        organizationId = null,
                        targetId = desired.id,
                        before = mapOf("globalCatalogId" to existing.globalCatalogId, "canonicalName" to existing.canonicalName, "status" to existing.status.name),
                        after = mapOf("globalCatalogId" to desired.globalCatalogId, "canonicalName" to desired.canonicalName, "status" to desired.status.name, "seed" to "initial"),
                        reason = command.reason,
                        createdAt = now,
                    )
                )
                CatalogSeedItemResult(CatalogSeedEntityType.TEMPLATE, desired.globalCatalogId, desired.id, CatalogSeedOutcome.UPDATED)
            }
        }
    }

    private fun PlatformCatalogTemplateRepository.findByGlobalCatalogId(globalCatalogId: String): PlatformCatalogTemplate? =
        search(
            CatalogTemplateSearchQuery(
                query = globalCatalogId.trim().lowercase(),
                onlyActive = false,
                limit = 200,
            )
        ).firstOrNull { it.globalCatalogId == globalCatalogId.trim().lowercase() }
}

data class CatalogInitialSeedCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val verticals: Set<CatalogInitialSeedVertical> = enumValues<CatalogInitialSeedVertical>().toSet(),
    val reason: String? = "Initial pilot catalog seed",
)

data class CatalogInitialSeedResult(
    val verticals: Set<CatalogInitialSeedVertical>,
    val items: List<CatalogSeedItemResult>,
) {
    val total: Int get() = items.size
    val created: Int get() = items.count { it.outcome == CatalogSeedOutcome.CREATED }
    val updated: Int get() = items.count { it.outcome == CatalogSeedOutcome.UPDATED }
    val unchanged: Int get() = items.count { it.outcome == CatalogSeedOutcome.UNCHANGED }
    val skipped: Int get() = items.count { it.outcome == CatalogSeedOutcome.SKIPPED }
}

data class CatalogSeedItemResult(
    val entityType: CatalogSeedEntityType,
    val code: String,
    val id: String,
    val outcome: CatalogSeedOutcome,
    val message: String? = null,
)

enum class CatalogInitialSeedVertical { RESTAURANT, TOURISM, RETAIL }
enum class CatalogSeedEntityType { CATEGORY, FAMILY, TEMPLATE }
enum class CatalogSeedOutcome { CREATED, UPDATED, UNCHANGED, SKIPPED }

private data class CatalogSeedCategoryDefinition(
    val vertical: CatalogInitialSeedVertical,
    val id: String,
    val code: String,
    val name: String,
    val parentCode: String? = null,
    val description: String? = null,
    val businessTypeTags: Set<String> = emptySet(),
    val activityTags: Set<String> = emptySet(),
    val sortOrder: Int = 0,
)

private data class CatalogSeedFamilyDefinition(
    val vertical: CatalogInitialSeedVertical,
    val id: String,
    val globalFamilyId: String,
    val canonicalName: String,
    val categoryCode: String? = null,
    val brand: String? = null,
    val type: CatalogItemType = CatalogItemType.PRODUCT,
    val aliases: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
)

private data class CatalogSeedTemplateDefinition(
    val vertical: CatalogInitialSeedVertical,
    val id: String,
    val globalCatalogId: String,
    val canonicalName: String,
    val type: CatalogItemType,
    val familyGlobalId: String? = null,
    val variantAttributes: Map<String, String> = emptyMap(),
    val identifiers: List<CatalogIdentifier> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
)

private object CatalogInitialSeedDefinitions {
    val categories: List<CatalogSeedCategoryDefinition> = listOf(
        CatalogSeedCategoryDefinition(
            vertical = CatalogInitialSeedVertical.RESTAURANT,
            id = "cat_seed_restaurant",
            code = "restaurant",
            name = "Restaurante",
            description = "Categorías base para operación de restaurante pequeño.",
            businessTypeTags = setOf("restaurant", "food_service"),
            activityTags = setOf("food_service"),
            sortOrder = 10,
        ),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RESTAURANT, "cat_seed_restaurant_main_dishes", "restaurant_main_dishes", "Platos fuertes", "restaurant", "Platos principales de venta rápida.", setOf("restaurant"), setOf("food_service"), 11),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RESTAURANT, "cat_seed_restaurant_soups", "restaurant_soups", "Sopas", "restaurant", "Sopas y entradas calientes.", setOf("restaurant"), setOf("food_service"), 12),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RESTAURANT, "cat_seed_restaurant_beverages", "restaurant_beverages", "Bebidas", "restaurant", "Bebidas frías, calientes y acompañantes.", setOf("restaurant", "cafe"), setOf("food_service"), 13),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RESTAURANT, "cat_seed_restaurant_extras", "restaurant_extras", "Extras", "restaurant", "Guarniciones y adicionales.", setOf("restaurant"), setOf("food_service"), 14),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RESTAURANT, "cat_seed_restaurant_packages", "restaurant_packages", "Combos y parrilladas", "restaurant", "Paquetes familiares o combinaciones.", setOf("restaurant"), setOf("food_service"), 15),

        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.TOURISM, "cat_seed_tourism", "tourism_experiences", "Turismo y experiencias", null, "Servicios turísticos, reservas y experiencias.", setOf("tourism", "experiences"), setOf("reservation", "service"), 20),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.TOURISM, "cat_seed_tourism_activities", "tourism_activities", "Actividades", "tourism_experiences", "Actividades reservables por persona, tiempo o cupo.", setOf("tourism"), setOf("reservation", "activity"), 21),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.TOURISM, "cat_seed_tourism_rentals", "tourism_rentals", "Alquiler de equipos", "tourism_experiences", "Alquiler por tiempo o jornada.", setOf("tourism", "rental"), setOf("reservation", "rental"), 22),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.TOURISM, "cat_seed_tourism_packages", "tourism_packages", "Paquetes turísticos", "tourism_experiences", "Combos de actividades y paquetes de experiencia.", setOf("tourism"), setOf("reservation", "package"), 23),

        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RETAIL, "cat_seed_retail", "retail_general", "Tienda general", null, "Productos básicos para tiendas, cafeterías y micromercados.", setOf("retail", "store"), setOf("sale"), 30),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RETAIL, "cat_seed_retail_beverages", "retail_beverages", "Bebidas de tienda", "retail_general", "Bebidas embotelladas o listas para venta.", setOf("retail", "store"), setOf("sale"), 31),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RETAIL, "cat_seed_retail_snacks", "retail_snacks", "Snacks", "retail_general", "Snacks y productos de impulso.", setOf("retail", "store"), setOf("sale"), 32),
        CatalogSeedCategoryDefinition(CatalogInitialSeedVertical.RETAIL, "cat_seed_retail_supplies", "retail_supplies", "Suministros", "retail_general", "Insumos básicos de operación o reventa.", setOf("retail", "store"), setOf("sale"), 33),
    )

    val families: List<CatalogSeedFamilyDefinition> = listOf(
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RESTAURANT, "pfam_seed_cuy", "restaurant_cuy", "Cuy preparado", "restaurant_main_dishes", null, CatalogItemType.PRODUCT, listOf("cuy", "cuy asado", "plato de cuy"), mapOf("defaultTaxProfileCode" to "iva_current_full")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RESTAURANT, "pfam_seed_borrego", "restaurant_borrego", "Borrego preparado", "restaurant_main_dishes", null, CatalogItemType.PRODUCT, listOf("borrego", "cordero"), mapOf("defaultTaxProfileCode" to "iva_current_full")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RESTAURANT, "pfam_seed_grill", "restaurant_grill", "Parrilladas", "restaurant_packages", null, CatalogItemType.PACKAGE, listOf("parrillada", "asado", "combo familiar"), mapOf("defaultTaxProfileCode" to "iva_current_full")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RESTAURANT, "pfam_seed_soup", "restaurant_soup", "Sopas de restaurante", "restaurant_soups", null, CatalogItemType.PRODUCT, listOf("sopa", "caldo"), mapOf("defaultTaxProfileCode" to "iva_current_full")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RESTAURANT, "pfam_seed_restaurant_beverage", "restaurant_beverage", "Bebidas de restaurante", "restaurant_beverages", null, CatalogItemType.PRODUCT, listOf("bebida", "jugo", "gaseosa"), mapOf("defaultTaxProfileCode" to "iva_current_full")),

        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.TOURISM, "pfam_seed_offroad", "tourism_offroad", "Actividad off-road", "tourism_activities", null, CatalogItemType.SERVICE, listOf("cuadrones", "atv", "off road"), mapOf("defaultTaxProfileCode" to "iva_current_full", "reservationRequired" to "true")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.TOURISM, "pfam_seed_paintball", "tourism_paintball", "Paintball", "tourism_activities", null, CatalogItemType.SERVICE, listOf("paintball", "actividad grupal"), mapOf("defaultTaxProfileCode" to "iva_current_full", "reservationRequired" to "true")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.TOURISM, "pfam_seed_camping", "tourism_camping", "Camping", "tourism_activities", null, CatalogItemType.SERVICE, listOf("camping", "noche", "carpa"), mapOf("defaultTaxProfileCode" to "iva_current_full", "reservationRequired" to "true")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.TOURISM, "pfam_seed_tourism_package", "tourism_package", "Paquetes de experiencia", "tourism_packages", null, CatalogItemType.PACKAGE, listOf("combo", "paquete turístico"), mapOf("defaultTaxProfileCode" to "iva_current_full", "reservationRequired" to "true")),

        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RETAIL, "pfam_seed_retail_beverage", "retail_beverage", "Bebidas embotelladas", "retail_beverages", null, CatalogItemType.PRODUCT, listOf("agua", "gaseosa", "bebida"), mapOf("defaultTaxProfileCode" to "iva_current_full")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RETAIL, "pfam_seed_retail_snack", "retail_snack", "Snacks de tienda", "retail_snacks", null, CatalogItemType.PRODUCT, listOf("snack", "papas", "galleta"), mapOf("defaultTaxProfileCode" to "iva_current_full")),
        CatalogSeedFamilyDefinition(CatalogInitialSeedVertical.RETAIL, "pfam_seed_retail_supply", "retail_supply", "Suministros básicos", "retail_supplies", null, CatalogItemType.PRODUCT, listOf("suministro", "desechable", "operación"), mapOf("defaultTaxProfileCode" to "iva_current_full")),
    )

    val templates: List<CatalogSeedTemplateDefinition> = listOf(
        restaurantTemplate("tpl_seed_cuy_entero", "restaurant_cuy_entero", "Cuy entero", "restaurant_cuy", "ALT-CUY-ENTERO", mapOf("portion" to "entero", "suggestedCategoryCode" to "restaurant_main_dishes")),
        restaurantTemplate("tpl_seed_medio_cuy", "restaurant_medio_cuy", "Medio cuy", "restaurant_cuy", "ALT-CUY-MEDIO", mapOf("portion" to "medio", "suggestedCategoryCode" to "restaurant_main_dishes")),
        restaurantTemplate("tpl_seed_borrego_asado", "restaurant_borrego_asado", "Borrego asado", "restaurant_borrego", "ALT-BORREGO", mapOf("suggestedCategoryCode" to "restaurant_main_dishes")),
        restaurantTemplate("tpl_seed_costilla_bbq", "restaurant_costilla_bbq", "Costilla BBQ", "restaurant_grill", "ALT-COSTILLA-BBQ", mapOf("suggestedCategoryCode" to "restaurant_main_dishes")),
        restaurantTemplate("tpl_seed_parrillada_individual", "restaurant_parrillada_individual", "Parrillada individual", "restaurant_grill", "ALT-PARRILLA-IND", mapOf("serves" to "1", "suggestedCategoryCode" to "restaurant_packages"), CatalogItemType.PACKAGE),
        restaurantTemplate("tpl_seed_parrillada_familiar", "restaurant_parrillada_familiar", "Parrillada familiar", "restaurant_grill", "ALT-PARRILLA-FAM", mapOf("serves" to "4", "suggestedCategoryCode" to "restaurant_packages"), CatalogItemType.PACKAGE),
        restaurantTemplate("tpl_seed_yahuarlocro", "restaurant_yahuarlocro", "Yahuarlocro", "restaurant_soup", "ALT-YAHUARLOCRO", mapOf("suggestedCategoryCode" to "restaurant_soups")),
        restaurantTemplate("tpl_seed_jugo_personal", "restaurant_jugo_personal", "Jugo personal", "restaurant_beverage", "ALT-JUGO-PER", mapOf("presentation" to "personal", "suggestedCategoryCode" to "restaurant_beverages")),
        restaurantTemplate("tpl_seed_jarra_jugo", "restaurant_jarra_jugo", "Jarra de jugo", "restaurant_beverage", "ALT-JUGO-JARRA", mapOf("presentation" to "jarra", "suggestedCategoryCode" to "restaurant_beverages")),
        restaurantTemplate("tpl_seed_gaseosa_personal", "restaurant_gaseosa_personal", "Gaseosa personal", "restaurant_beverage", "ALT-GASEOSA-PER", mapOf("presentation" to "personal", "suggestedCategoryCode" to "restaurant_beverages")),

        tourismTemplate("tpl_seed_offroad_1h", "tourism_offroad_1h", "Cuadrón off-road 1 hora", "tourism_offroad", "ALT-OFFROAD-1H", mapOf("durationMinutes" to "60", "capacityUnit" to "vehicle", "suggestedCategoryCode" to "tourism_activities"), CatalogItemType.SERVICE),
        tourismTemplate("tpl_seed_paintball_30m", "tourism_paintball_30m", "Paintball 30 minutos", "tourism_paintball", "ALT-PAINTBALL-30M", mapOf("durationMinutes" to "30", "capacityUnit" to "person", "suggestedCategoryCode" to "tourism_activities"), CatalogItemType.SERVICE),
        tourismTemplate("tpl_seed_gokarts_30m", "tourism_gokarts_30m", "Go karts 30 minutos", "tourism_offroad", "ALT-GOKARTS-30M", mapOf("durationMinutes" to "30", "capacityUnit" to "person", "suggestedCategoryCode" to "tourism_activities"), CatalogItemType.SERVICE),
        tourismTemplate("tpl_seed_shooting_30m", "tourism_shooting_range_30m", "Polígono de tiro 30 minutos", "tourism_paintball", "ALT-SHOOTING-30M", mapOf("durationMinutes" to "30", "capacityUnit" to "person", "suggestedCategoryCode" to "tourism_activities"), CatalogItemType.SERVICE),
        tourismTemplate("tpl_seed_camping_night", "tourism_camping_night", "Camping por noche", "tourism_camping", "ALT-CAMPING-NIGHT", mapOf("durationUnit" to "night", "capacityUnit" to "person", "suggestedCategoryCode" to "tourism_activities"), CatalogItemType.SERVICE),
        tourismTemplate("tpl_seed_experience_combo", "tourism_experience_combo", "Combo experiencia", "tourism_package", "ALT-COMBO-EXP", mapOf("combo" to "true", "suggestedCategoryCode" to "tourism_packages"), CatalogItemType.PACKAGE),

        retailTemplate("tpl_seed_water_500ml", "retail_water_500ml", "Agua embotellada 500 ml", "retail_beverage", "GEN-AGUA-500", mapOf("presentation" to "500 ml", "suggestedCategoryCode" to "retail_beverages")),
        retailTemplate("tpl_seed_soda_500ml", "retail_soda_500ml", "Gaseosa 500 ml", "retail_beverage", "GEN-GASEOSA-500", mapOf("presentation" to "500 ml", "suggestedCategoryCode" to "retail_beverages")),
        retailTemplate("tpl_seed_snack_small", "retail_snack_small", "Snack pequeño", "retail_snack", "GEN-SNACK-S", mapOf("presentation" to "pequeño", "suggestedCategoryCode" to "retail_snacks")),
        retailTemplate("tpl_seed_cookie_pack", "retail_cookie_pack", "Paquete de galletas", "retail_snack", "GEN-GALLETA-PACK", mapOf("suggestedCategoryCode" to "retail_snacks")),
        retailTemplate("tpl_seed_disposable_plate", "retail_disposable_plate", "Plato desechable", "retail_supply", "GEN-PLATO-DES", mapOf("suggestedCategoryCode" to "retail_supplies")),
        retailTemplate("tpl_seed_charcoal_bag", "retail_charcoal_bag", "Carbón en funda", "retail_supply", "GEN-CARBON", mapOf("suggestedCategoryCode" to "retail_supplies")),
    )

    private fun restaurantTemplate(
        id: String,
        globalCatalogId: String,
        name: String,
        family: String,
        code: String,
        attributes: Map<String, String>,
        type: CatalogItemType = CatalogItemType.PRODUCT,
    ): CatalogSeedTemplateDefinition = template(
        vertical = CatalogInitialSeedVertical.RESTAURANT,
        id = id,
        globalCatalogId = globalCatalogId,
        name = name,
        family = family,
        code = code,
        type = type,
        attributes = attributes + mapOf("businessType" to "restaurant", "defaultTaxProfileCode" to "iva_current_full"),
    )

    private fun tourismTemplate(
        id: String,
        globalCatalogId: String,
        name: String,
        family: String,
        code: String,
        attributes: Map<String, String>,
        type: CatalogItemType,
    ): CatalogSeedTemplateDefinition = template(
        vertical = CatalogInitialSeedVertical.TOURISM,
        id = id,
        globalCatalogId = globalCatalogId,
        name = name,
        family = family,
        code = code,
        type = type,
        attributes = attributes + mapOf("businessType" to "tourism", "defaultTaxProfileCode" to "iva_current_full", "reservationRequired" to "true"),
    )

    private fun retailTemplate(
        id: String,
        globalCatalogId: String,
        name: String,
        family: String,
        code: String,
        attributes: Map<String, String>,
    ): CatalogSeedTemplateDefinition = template(
        vertical = CatalogInitialSeedVertical.RETAIL,
        id = id,
        globalCatalogId = globalCatalogId,
        name = name,
        family = family,
        code = code,
        type = CatalogItemType.PRODUCT,
        attributes = attributes + mapOf("businessType" to "retail", "defaultTaxProfileCode" to "iva_current_full"),
    )

    private fun template(
        vertical: CatalogInitialSeedVertical,
        id: String,
        globalCatalogId: String,
        name: String,
        family: String,
        code: String,
        type: CatalogItemType,
        attributes: Map<String, String>,
    ): CatalogSeedTemplateDefinition = CatalogSeedTemplateDefinition(
        vertical = vertical,
        id = id,
        globalCatalogId = globalCatalogId,
        canonicalName = name,
        type = type,
        familyGlobalId = family,
        identifiers = listOf(
            CatalogIdentifier.create(
                type = CatalogIdentifierType.SKU_MASTER,
                value = code,
                scope = CatalogIdentifierScope.GLOBAL,
                source = CatalogIdentifierSource.PLATFORM,
                status = CatalogIdentifierStatus.ACTIVE,
                isPrimary = true,
            )
        ),
        attributes = attributes,
    )
}

private fun CatalogCategory.seedEquivalentTo(other: CatalogCategory): Boolean =
    parentId == other.parentId &&
        code == other.code &&
        name == other.name &&
        normalizedName == other.normalizedName &&
        description == other.description &&
        businessTypeTags == other.businessTypeTags &&
        activityTags == other.activityTags &&
        status == other.status &&
        sortOrder == other.sortOrder

private fun PlatformCatalogFamily.seedEquivalentTo(other: PlatformCatalogFamily): Boolean =
    globalFamilyId == other.globalFamilyId &&
        canonicalName == other.canonicalName &&
        normalizedName == other.normalizedName &&
        categoryId == other.categoryId &&
        brand == other.brand &&
        type == other.type &&
        aliases == other.aliases &&
        attributes == other.attributes &&
        status == other.status

private fun PlatformCatalogTemplate.seedEquivalentTo(other: PlatformCatalogTemplate): Boolean =
    globalCatalogId == other.globalCatalogId &&
        canonicalName == other.canonicalName &&
        normalizedName == other.normalizedName &&
        type == other.type &&
        status == other.status &&
        productFamilyId == other.productFamilyId &&
        variantAttributes == other.variantAttributes &&
        identifiers == other.identifiers &&
        attributes == other.attributes

private fun String.normalizedSearchText(): String = trim().lowercase().replace(Regex("\\s+"), " ")
