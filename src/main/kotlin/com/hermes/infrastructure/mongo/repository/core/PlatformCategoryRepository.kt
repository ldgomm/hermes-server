package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class PlatformCategoryRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.PLATFORM_CATEGORIES) {
    fun findBySlug(slug: String): Document? = findOne(eq("slug", slug.trim()))
    fun findByParent(parentId: String?): List<Document> = findMany(eq("parentId", parentId))
}
