package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import org.bson.Document

class CounterRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.COUNTERS) {
    fun findByScopeAndKey(organizationId: String, scope: String, key: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("scope", scope), eq("key", key)))

    fun incrementAndGet(organizationId: String, scope: String, key: String, increment: Long = 1): Long {
        val updated = collection.findOneAndUpdate(
            and(organizationFilter(organizationId), eq("scope", scope), eq("key", key)),
            Updates.inc("currentValue", increment),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw IllegalStateException("Counter $scope/$key does not exist for organization $organizationId.")
        return when (val raw = updated["currentValue"]) {
            is Int -> raw.toLong()
            is Long -> raw
            is Number -> raw.toLong()
            else -> error("Counter currentValue is not numeric.")
        }
    }
}
