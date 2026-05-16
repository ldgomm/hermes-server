package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class PlatformCatalogFamilyRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.PLATFORM_CATALOG_FAMILIES) {
    fun findByGlobalFamilyId(globalFamilyId: String): Document? = findOne(eq("globalFamilyId", globalFamilyId.trim()))
    fun findByCategory(categoryCode: String): List<Document> = findMany(eq("categoryCode", categoryCode.trim()))
}
