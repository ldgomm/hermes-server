package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class PermissionRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.PERMISSIONS,
) {
    fun findByCode(code: String): Document? = findOne(eq("code", code.trim()))

    fun findByModule(module: String): List<Document> = findMany(eq("module", module.trim()))
}
