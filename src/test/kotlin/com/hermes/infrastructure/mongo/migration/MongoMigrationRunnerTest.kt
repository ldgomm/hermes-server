package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MongoMigrationRunnerTest {
    @Test
    fun `applies pending migrations once and records metadata`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("migration_test"))
            val runner = MongoMigrationRunner(database)

            val migrations = listOf(
                object : MongoMigration {
                    override val id: String = "M001_create_probe_collection"
                    override val description: String = "Create probe collection"

                    override fun up(database: MongoDatabase) {
                        database.createCollection("probe_entities")
                        database.getCollection("probe_entities").createIndex(
                            Indexes.ascending("organizationId", "code"),
                            IndexOptions().unique(true).name("probe_org_code_unique_idx"),
                        )
                    }
                },
            )

            val firstRun = runner.migrate(migrations)
            val secondRun = runner.migrate(migrations)

            assertEquals(1, firstRun.appliedCount)
            assertEquals(0, secondRun.appliedCount)
            assertEquals(1, secondRun.alreadyApplied)
            assertEquals(1, database.getCollection("schema_migrations").countDocuments())
        }
    }

    @Test
    fun `rejects duplicate migration ids`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("migration_validation_test"))
            val runner = MongoMigrationRunner(database)
            val duplicate = object : MongoMigration {
                override val id: String = "M001_duplicate"
                override val description: String = "Duplicate"
                override fun up(database: MongoDatabase) = Unit
            }

            assertFailsWith<MongoMigrationValidationException> {
                runner.migrate(listOf(duplicate, duplicate))
            }
        }
    }
}
