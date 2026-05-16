package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import org.bson.Document
import java.util.*

class CounterRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.COUNTERS) {
    fun findByScopeAndType(
        organizationId: String,
        scope: String,
        scopeId: String,
        counterType: String,
    ): Document? = findOne(
        and(
            organizationFilter(organizationId),
            eq("scope", scope.trim()),
            eq("scopeId", scopeId.trim()),
            eq("counterType", counterType.trim()),
        )
    )

    fun incrementAndGet(
        organizationId: String,
        scope: String,
        scopeId: String,
        counterType: String,
        increment: Int = 1,
        issuedAt: Date = Date(),
    ): Long {
        require(increment > 0) { "Counter increment must be greater than zero." }

        val updated = collection.findOneAndUpdate(
            and(
                organizationFilter(organizationId),
                eq("scope", scope.trim()),
                eq("scopeId", scopeId.trim()),
                eq("counterType", counterType.trim()),
                eq("status", "active"),
            ),
            Updates.combine(
                Updates.inc("current", increment),
                Updates.set("lastIssuedAt", issuedAt),
                Updates.inc("version", 1),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw IllegalStateException(
            "Active counter $scope/$scopeId/$counterType does not exist for organization $organizationId."
        )

        return when (val raw = updated["current"]) {
            is Int -> raw.toLong()
            is Long -> raw
            is Number -> raw.toLong()
            else -> error("Counter current is not numeric.")
        }
    }

    /**
     * Backwards-compatible adapter for early Phase 4.2 callers.
     * Prefer findByScopeAndType because the Mongo schema stores scopeId + counterType, not a generic key field.
     */
    @Deprecated("Use findByScopeAndType(organizationId, scope, scopeId, counterType).")
    fun findByScopeAndKey(organizationId: String, scope: String, key: String): Document? =
        findByScopeAndType(
            organizationId = organizationId,
            scope = scope,
            scopeId = organizationId,
            counterType = key,
        )

    /**
     * Backwards-compatible adapter for early Phase 4.2 callers.
     * Prefer incrementAndGet(organizationId, scope, scopeId, counterType, increment).
     */
    @Deprecated("Use incrementAndGet(organizationId, scope, scopeId, counterType, increment).")
    fun incrementAndGet(organizationId: String, scope: String, key: String, increment: Long = 1L): Long =
        incrementAndGet(
            organizationId = organizationId,
            scope = scope,
            scopeId = organizationId,
            counterType = key,
            increment = increment.toInt(),
        )
}
