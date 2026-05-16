package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document
import java.security.MessageDigest
import java.time.Instant
import java.util.*
import kotlin.system.measureTimeMillis

class MongoMigrationRunner(
    private val database: MongoDatabase,
    private val repository: MongoMigrationRepository = MongoMigrationRepository(database),
    private val lockTtlSeconds: Long = 300,
) {
    fun migrate(migrations: List<MongoMigration>): MongoMigrationRunReport {
        validate(migrations)
        repository.ensureCollections()

        return withLock {
            val appliedBefore = repository.appliedIds()
            val pending = migrations.sortedBy { it.id }.filterNot { it.id in appliedBefore }
            val appliedReports = mutableListOf<MongoMigrationAppliedReport>()

            pending.forEach { migration ->
                val checksum = checksum(migration)
                val elapsed = measureTimeMillis {
                    try {
                        migration.up(database)
                    } catch (error: Throwable) {
                        throw MongoMigrationFailedException(migration.id, error)
                    }
                }

                repository.recordApplied(
                    migration = migration,
                    checksum = checksum,
                    executionMillis = elapsed,
                    appliedAt = Instant.now(),
                )
                appliedReports += MongoMigrationAppliedReport(
                    id = migration.id,
                    description = migration.description,
                    executionMillis = elapsed,
                )
            }

            MongoMigrationRunReport(
                totalMigrations = migrations.size,
                alreadyApplied = appliedBefore.size,
                applied = appliedReports,
            )
        }
    }

    private fun validate(migrations: List<MongoMigration>) {
        val ids = migrations.map { it.id }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw MongoMigrationValidationException("Duplicate Mongo migration ids: ${duplicates.joinToString()}.")
        }

        migrations.forEach { migration ->
            if (!MIGRATION_ID_PATTERN.matches(migration.id)) {
                throw MongoMigrationValidationException(
                    "Mongo migration id '${migration.id}' is invalid. Expected format: M001_short_description.",
                )
            }
            if (migration.description.isBlank()) {
                throw MongoMigrationValidationException("Mongo migration '${migration.id}' description cannot be blank.")
            }
        }
    }

    private fun <T> withLock(block: () -> T): T {
        val locks = database.getCollection(MongoCollectionNames.SCHEMA_MIGRATION_LOCKS)
        val lockId = "global"
        val owner = UUID.randomUUID().toString()
        val now = Instant.now()

        try {
            locks.insertOne(
                Document(MongoDocumentFields.ID, lockId)
                    .append("owner", owner)
                    .append("lockedAt", Date.from(now))
                    .append("expiresAt", Date.from(now.plusSeconds(lockTtlSeconds)))
                    .append(MongoDocumentFields.SCHEMA_VERSION, 1),
            )
        } catch (error: MongoWriteException) {
            if (error.error.category.name == "DUPLICATE_KEY") {
                throw MongoMigrationLockException("Another Mongo migration runner is already active.", error)
            }
            throw error
        }

        try {
            return block()
        } finally {
            locks.deleteOne(eq(MongoDocumentFields.ID, lockId))
        }
    }

    private fun checksum(migration: MongoMigration): String {
        val input = "${migration.id}:${migration.description}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    companion object {
        private val MIGRATION_ID_PATTERN = Regex("^M[0-9]{3}_[a-z0-9_]+$")
    }
}
