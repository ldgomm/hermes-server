package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.Document
import java.time.Instant
import java.util.Date

class MongoMigrationRepository(
    private val database: MongoDatabase,
) {
    private val collection: MongoCollection<Document>
        get() = database.getCollection(MongoCollectionNames.SCHEMA_MIGRATIONS)

    fun ensureCollections() {
        val names = database.listCollectionNames().toSet()
        if (MongoCollectionNames.SCHEMA_MIGRATIONS !in names) {
            database.createCollection(MongoCollectionNames.SCHEMA_MIGRATIONS)
        }
        if (MongoCollectionNames.SCHEMA_MIGRATION_LOCKS !in names) {
            database.createCollection(MongoCollectionNames.SCHEMA_MIGRATION_LOCKS)
        }

        collection.createIndex(Indexes.ascending("appliedAt"))
        collection.createIndex(
            Indexes.ascending("checksum"),
            IndexOptions().name("schema_migrations_checksum_idx"),
        )
    }

    fun appliedIds(): Set<String> =
        collection.find()
            .projection(Document(MongoDocumentFields.ID, 1))
            .map { it.getString(MongoDocumentFields.ID) }
            .toSet()

    fun hasApplied(id: String): Boolean =
        collection.countDocuments(eq(MongoDocumentFields.ID, id)) > 0

    fun recordApplied(
        migration: MongoMigration,
        checksum: String,
        executionMillis: Long,
        appliedAt: Instant,
    ) {
        collection.insertOne(
            Document(MongoDocumentFields.ID, migration.id)
                .append("description", migration.description)
                .append("checksum", checksum)
                .append("executionMillis", executionMillis)
                .append("appliedAt", Date.from(appliedAt))
                .append(MongoDocumentFields.SCHEMA_VERSION, 1),
        )
    }
}
