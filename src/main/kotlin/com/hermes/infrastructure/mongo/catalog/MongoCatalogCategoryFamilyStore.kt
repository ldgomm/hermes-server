package com.hermes.infrastructure.mongo.catalog

import com.hermes.application.catalog.CatalogCategoryRepository
import com.hermes.application.catalog.CatalogCategorySearchQuery
import com.hermes.application.catalog.PlatformCatalogFamilyRepository
import com.hermes.application.catalog.PlatformCatalogFamilySearchQuery
import com.hermes.domain.catalog.*
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.Document

class MongoCatalogCategoryFamilyStore(database: MongoDatabase) {
    val categoryRepository: CatalogCategoryRepository = MongoCatalogCategoryRepository(database)
    val familyRepository: PlatformCatalogFamilyRepository = MongoPlatformCatalogFamilyRepository(database)
}

private class MongoCatalogCategoryRepository(database: MongoDatabase) : CatalogCategoryRepository {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.PLATFORM_CATEGORIES)

    override fun create(category: CatalogCategory) {
        collection.insertOne(category.toDocument())
    }

    override fun update(category: CatalogCategory) {
        collection.replaceOne(eq("_id", category.id), category.toDocument(), ReplaceOptions().upsert(false))
    }

    override fun findById(id: String): CatalogCategory? =
        collection.find(eq("_id", id.trim())).firstOrNull()?.toCatalogCategory()

    override fun findByCode(code: String): CatalogCategory? =
        collection.find(eq("code", code.trim().lowercase())).firstOrNull()?.toCatalogCategory()

    override fun existsByCode(code: String): Boolean =
        collection.find(eq("code", code.trim().lowercase())).limit(1).firstOrNull() != null

    override fun search(query: CatalogCategorySearchQuery): List<CatalogCategory> {
        val filters = mutableListOf<org.bson.conversions.Bson>()
        query.parentId?.takeIf { it.isNotBlank() }?.let { filters += eq("parentId", it) }
        if (query.statuses.isNotEmpty()) filters += com.mongodb.client.model.Filters.`in`(
            "status", query.statuses.map { it.name })
        query.query?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            filters += or(
                regex("name", text, "i"),
                regex("normalizedName", text.lowercase(), "i"),
                regex("code", text.lowercase(), "i")
            )
        }
        val finalFilter = if (filters.isEmpty()) Document() else and(filters)
        return collection.find(finalFilter).sort(Sorts.ascending("sortOrder", "code"))
            .limit(query.limit.coerceIn(1, 200)).into(mutableListOf()).map { it.toCatalogCategory() }
    }
}

private class MongoPlatformCatalogFamilyRepository(database: MongoDatabase) : PlatformCatalogFamilyRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.PLATFORM_CATALOG_FAMILIES)

    override fun create(family: PlatformCatalogFamily) {
        collection.insertOne(family.toDocument())
    }

    override fun update(family: PlatformCatalogFamily) {
        collection.replaceOne(eq("_id", family.id), family.toDocument(), ReplaceOptions().upsert(false))
    }

    override fun findById(id: String): PlatformCatalogFamily? =
        collection.find(eq("_id", id.trim())).firstOrNull()?.toPlatformCatalogFamily()

    override fun findByGlobalFamilyId(globalFamilyId: String): PlatformCatalogFamily? =
        collection.find(eq("globalFamilyId", globalFamilyId.trim().lowercase())).firstOrNull()
            ?.toPlatformCatalogFamily()

    override fun existsByGlobalFamilyId(globalFamilyId: String): Boolean =
        collection.find(eq("globalFamilyId", globalFamilyId.trim().lowercase())).limit(1).firstOrNull() != null

    override fun search(query: PlatformCatalogFamilySearchQuery): List<PlatformCatalogFamily> {
        val filters = mutableListOf<org.bson.conversions.Bson>()
        query.categoryId?.takeIf { it.isNotBlank() }?.let { filters += eq("categoryId", it) }
        query.type?.let { filters += eq("type", it.name) }
        if (query.statuses.isNotEmpty()) filters += com.mongodb.client.model.Filters.`in`(
            "status", query.statuses.map { it.name })
        query.query?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            filters += or(
                regex("canonicalName", text, "i"),
                regex("normalizedName", text.lowercase(), "i"),
                regex("globalFamilyId", text.lowercase(), "i"),
                regex("aliases", text, "i")
            )
        }
        val finalFilter = if (filters.isEmpty()) Document() else and(filters)
        return collection.find(finalFilter).sort(Sorts.ascending("canonicalName")).limit(query.limit.coerceIn(1, 200))
            .into(mutableListOf()).map { it.toPlatformCatalogFamily() }
    }
}

private fun CatalogCategory.toDocument(): Document =
    Document(MongoDocumentFields.ID, id).append("parentId", parentId).append("code", code).append("name", name)
        .append("normalizedName", normalizedName).append("description", description)
        .append("businessTypeTags", businessTypeTags.toList()).append("activityTags", activityTags.toList())
        .append("status", status.name).append("sortOrder", sortOrder)
        .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(createdAt))
        .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(updatedAt))
        .append(MongoDocumentFields.VERSION, version.toInt())

private fun Document.toCatalogCategory(): CatalogCategory = CatalogCategory(
    id = requiredString(MongoDocumentFields.ID),
    parentId = optionalString("parentId"),
    code = requiredString("code"),
    name = requiredString("name"),
    normalizedName = requiredString("normalizedName"),
    description = optionalString("description"),
    businessTypeTags = stringSet("businessTypeTags"),
    activityTags = stringSet("activityTags"),
    status = optionalEnum("status", CatalogCategoryStatus.ACTIVE),
    sortOrder = getInteger("sortOrder", 0),
    createdAt = MongoInstantMapper.readRequired(this, MongoDocumentFields.CREATED_AT),
    updatedAt = MongoInstantMapper.readRequired(this, MongoDocumentFields.UPDATED_AT),
    version = optionalLong(MongoDocumentFields.VERSION, 1),
)

private fun PlatformCatalogFamily.toDocument(): Document =
    Document(MongoDocumentFields.ID, id).append("globalFamilyId", globalFamilyId).append("canonicalName", canonicalName)
        .append("normalizedName", normalizedName).append("categoryId", categoryId).append("brand", brand)
        .append("type", type.name).append("aliases", aliases).append("attributes", Document(attributes))
        .append("status", status.name).append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(createdAt))
        .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(updatedAt))
        .append(MongoDocumentFields.VERSION, version.toInt())

private fun Document.toPlatformCatalogFamily(): PlatformCatalogFamily = PlatformCatalogFamily(
    id = requiredString(MongoDocumentFields.ID),
    globalFamilyId = requiredString("globalFamilyId"),
    canonicalName = requiredString("canonicalName"),
    normalizedName = requiredString("normalizedName"),
    categoryId = optionalString("categoryId") ?: optionalString("categoryCode"),
    brand = optionalString("brand"),
    type = optionalEnum("type", optionalEnum("itemType", CatalogItemType.PRODUCT)),
    aliases = stringList("aliases"),
    attributes = documentToStringMap("attributes"),
    status = optionalEnum("status", CatalogTemplateStatus.ACTIVE),
    createdAt = MongoInstantMapper.readOptional(this, MongoDocumentFields.CREATED_AT) ?: java.time.Instant.EPOCH,
    updatedAt = MongoInstantMapper.readOptional(this, MongoDocumentFields.UPDATED_AT) ?: java.time.Instant.EPOCH,
    version = optionalLong(MongoDocumentFields.VERSION, 1),
)

private fun Document.requiredString(field: String): String = getString(field)?.takeIf { it.isNotBlank() }
    ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

private fun Document.optionalString(field: String): String? = getString(field)?.takeIf { it.isNotBlank() }

private fun Document.optionalLong(field: String, default: Long): Long = when (val raw = this[field]) {
    null -> default
    is Int -> raw.toLong()
    is Long -> raw
    else -> default
}

private inline fun <reified T : Enum<T>> Document.optionalEnum(field: String, default: T): T =
    optionalString(field)?.let { enumValueOf<T>(it.uppercase()) } ?: default

private fun Document.stringList(field: String): List<String> =
    (this[field] as? List<*>)?.filterIsInstance<String>().orEmpty()

private fun Document.stringSet(field: String): Set<String> = stringList(field).toSet()

private fun Document.documentToStringMap(field: String): Map<String, String> =
    (get(field, Document::class.java) ?: Document()).mapValues { it.value.toString() }
