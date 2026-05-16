package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class CatalogImportJobRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.CATALOG_IMPORT_JOBS) {
    fun findByObjectKey(objectKey: String): Document? = findOne(eq("objectKey", objectKey))
    fun findByUploader(organizationId: String, uploadedBy: String): List<Document> = findMany(
        and(organizationFilter(organizationId), eq("uploadedBy", uploadedBy)),
        sort = Sorts.descending("createdAt")
    )
}
