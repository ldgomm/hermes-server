package com.hermes.infrastructure.mongo.repository

import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document

abstract class BaseMongoRepository<T : Any>(
    database: MongoDatabase,
    val collectionName: String,
) {
    protected val collection: MongoCollection<Document> = database.getCollection(collectionName)

    protected abstract fun toDocument(entity: T): Document

    protected abstract fun fromDocument(document: Document): T

    protected abstract fun entityId(entity: T): String

    protected open fun entityVersion(entity: T): Long? = null

    fun findById(id: String): T? =
        collection.find(eq(MongoDocumentFields.ID, id)).firstOrNull()?.let(::fromDocument)

    fun existsById(id: String): Boolean =
        collection.countDocuments(eq(MongoDocumentFields.ID, id)) > 0

    fun insert(entity: T): T {
        val document = toDocument(entity)
        val id = entityId(entity)
        try {
            collection.insertOne(document)
        } catch (error: MongoWriteException) {
            if (error.error.category.name == "DUPLICATE_KEY") {
                throw MongoDuplicateEntityException(collectionName, id, error)
            }
            throw error
        }
        return entity
    }

    fun replace(entity: T, expectedVersion: Long? = entityVersion(entity)): T {
        val id = entityId(entity)
        val document = toDocument(entity)
        val filter = if (expectedVersion == null) {
            eq(MongoDocumentFields.ID, id)
        } else {
            and(eq(MongoDocumentFields.ID, id), eq(MongoDocumentFields.VERSION, expectedVersion))
        }

        val result = collection.replaceOne(filter, document, ReplaceOptions().upsert(false))
        if (result.matchedCount == 0L) {
            if (expectedVersion == null) {
                throw MongoEntityNotFoundException(collectionName, id)
            }
            throw MongoOptimisticLockException(collectionName, id, expectedVersion)
        }

        return entity
    }

    fun requireById(id: String): T =
        findById(id) ?: throw MongoEntityNotFoundException(collectionName, id)
}
