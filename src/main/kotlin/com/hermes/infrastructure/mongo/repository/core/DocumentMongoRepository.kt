package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.repository.BaseMongoRepository
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.conversions.Bson
import java.util.*

abstract class DocumentMongoRepository(
    database: MongoDatabase,
    collectionName: String,
) : BaseMongoRepository<Document>(database, collectionName) {
    override fun toDocument(entity: Document): Document = Document(entity)

    override fun fromDocument(document: Document): Document = Document(document)

    override fun entityId(entity: Document): String = entity.getString(MongoDocumentFields.ID)
        ?: throw IllegalArgumentException("Mongo document in '$collectionName' requires '${MongoDocumentFields.ID}'.")

    override fun entityVersion(entity: Document): Long? {
        return when (val raw = entity[MongoDocumentFields.VERSION]) {
            is Int -> raw.toLong()
            is Long -> raw
            is Number -> raw.toLong()
            else -> null
        }
    }

    fun count(): Long = collection.countDocuments()

    fun count(filter: Bson): Long = collection.countDocuments(filter)

    fun findAll(
        limit: Int = DEFAULT_LIMIT,
        sort: Bson = Sorts.descending(MongoDocumentFields.CREATED_AT),
    ): List<Document> = findMany(
        filter = Document(),
        sort = sort,
        limit = limit,
    )

    fun findByOrganization(
        organizationId: String,
        limit: Int = DEFAULT_LIMIT,
        sort: Bson = Sorts.descending(MongoDocumentFields.CREATED_AT),
    ): List<Document> = findMany(
        filter = organizationFilter(organizationId),
        sort = sort,
        limit = limit,
    )

    fun findByStatus(
        status: String,
        limit: Int = DEFAULT_LIMIT,
        sort: Bson = Sorts.descending(MongoDocumentFields.CREATED_AT),
    ): List<Document> = findMany(
        filter = eq(MongoDocumentFields.STATUS, status),
        sort = sort,
        limit = limit,
    )

    fun findByOrganizationAndStatus(
        organizationId: String,
        status: String,
        limit: Int = DEFAULT_LIMIT,
        sort: Bson = Sorts.descending(MongoDocumentFields.CREATED_AT),
    ): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq(MongoDocumentFields.STATUS, status)),
        sort = sort,
        limit = limit,
    )

    fun findCreatedBetween(
        organizationId: String,
        from: Date,
        to: Date,
        limit: Int = DEFAULT_LIMIT,
    ): List<Document> = findMany(
        filter = and(
            organizationFilter(organizationId),
            gte(MongoDocumentFields.CREATED_AT, from),
            lte(MongoDocumentFields.CREATED_AT, to),
        ),
        sort = Sorts.descending(MongoDocumentFields.CREATED_AT),
        limit = limit,
    )

    fun softArchive(
        id: String,
        archivedAt: Date,
        archivedBy: String,
    ): Document? = collection.findOneAndUpdate(
        eq(MongoDocumentFields.ID, id),
        Updates.combine(
            Updates.set(MongoDocumentFields.STATUS, "archived"),
            Updates.set(MongoDocumentFields.ARCHIVED_AT, archivedAt),
            Updates.set(MongoDocumentFields.UPDATED_AT, archivedAt),
            Updates.set(MongoDocumentFields.UPDATED_BY, archivedBy),
            Updates.inc(MongoDocumentFields.VERSION, 1),
        ),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
    )?.let(::fromDocument)

    protected fun findOne(filter: Bson): Document? =
        collection.find(filter).firstOrNull()?.let(::fromDocument)

    protected fun findMany(
        filter: Bson,
        sort: Bson = Sorts.descending(MongoDocumentFields.CREATED_AT),
        limit: Int = DEFAULT_LIMIT,
    ): List<Document> = collection
        .find(filter)
        .sort(sort)
        .limit(limit.coerceIn(1, MAX_LIMIT))
        .map(::fromDocument)
        .toList()

    protected fun findOneByOrganizationAndField(
        organizationId: String,
        field: String,
        value: Any?,
    ): Document? = findOne(and(organizationFilter(organizationId), eq(field, value)))

    protected fun findManyByOrganizationAndField(
        organizationId: String,
        field: String,
        value: Any?,
        limit: Int = DEFAULT_LIMIT,
        sort: Bson = Sorts.descending(MongoDocumentFields.CREATED_AT),
    ): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq(field, value)),
        sort = sort,
        limit = limit,
    )

    protected fun organizationFilter(organizationId: String): Bson =
        eq(MongoDocumentFields.ORGANIZATION_ID, organizationId)

    protected fun activeStatuses(): Bson =
        `in`(MongoDocumentFields.STATUS, listOf("active", "published", "open", "pending", "processing"))

    protected fun requireFieldExists(field: String): Bson = exists(field, true)

    companion object {
        const val DEFAULT_LIMIT: Int = 100
        const val MAX_LIMIT: Int = 500
    }
}
