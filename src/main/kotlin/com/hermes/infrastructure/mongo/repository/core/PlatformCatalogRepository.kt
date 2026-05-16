package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.regex.Pattern

class PlatformCatalogRepository(
    database: MongoDatabase,
) {
    private val categories: MongoCollection<Document> = database.getCollection(MongoCollectionNames.PLATFORM_CATEGORIES)
    private val families: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.PLATFORM_CATALOG_FAMILIES)
    private val templates: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES)

    fun insertCategory(document: Document): Document = document.also { categories.insertOne(Document(it)) }

    fun insertFamily(document: Document): Document = document.also { families.insertOne(Document(it)) }

    fun insertTemplate(document: Document): Document = document.also { templates.insertOne(Document(it)) }

    fun findCategoryBySlug(slug: String): Document? =
        categories.find(eq("slug", slug.trim())).firstOrNull()?.let(::Document)

    fun findFamilyByGlobalFamilyId(globalFamilyId: String): Document? = families
        .find(eq("globalFamilyId", globalFamilyId.trim()))
        .firstOrNull()
        ?.let(::Document)

    fun findTemplateByGlobalCatalogId(globalCatalogId: String): Document? = templates
        .find(eq("globalCatalogId", globalCatalogId.trim()))
        .firstOrNull()
        ?.let(::Document)

    fun findTemplatesByIdentifier(normalizedValue: String, limit: Int = 100): List<Document> = templates
        .find(eq("identifiers.normalizedValue", normalizedValue.trim()))
        .sort(Sorts.ascending("canonicalName"))
        .limit(limit.coerceIn(1, 500))
        .map(::Document)
        .toList()

    fun searchTemplatesByName(query: String, limit: Int = 100): List<Document> = templates
        .find(regex("normalizedName", Pattern.quote(query.trim().lowercase()), "i"))
        .sort(Sorts.ascending("canonicalName"))
        .limit(limit.coerceIn(1, 500))
        .map(::Document)
        .toList()

    fun findTemplatesByFamily(productFamilyId: String, limit: Int = 100): List<Document> = templates
        .find(and(eq("productFamilyId", productFamilyId), eq("status", "published")))
        .sort(Sorts.ascending("canonicalName"))
        .limit(limit.coerceIn(1, 500))
        .map(::Document)
        .toList()

    fun countCategories(): Long = categories.countDocuments()

    fun countFamilies(): Long = families.countDocuments()

    fun countTemplates(): Long = templates.countDocuments()
}
